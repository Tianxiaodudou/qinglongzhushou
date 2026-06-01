package com.qinglong.app.data.api

import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * 青龙面板 WebSocket 管理器
 *
 * 青龙面板使用 SockJS 协议实现 WebSocket 通信，路径为 /api/ws。
 * 本类按照 SockJS 的 WebSocket 传输协议手动实现连接：
 * 1. GET /api/ws/info 获取服务器信息
 * 2. 生成随机 session_id
 * 3. 连接 WebSocket 到 /api/ws/<server_id>/<session_id>/websocket
 * 4. 发送/接收 JSON 数组格式的消息
 *
 * 消息类型（SockMessageType）：
 * - manuallyRunScript: 手动运行脚本的实时日志
 * - runSubscriptionEnd: 订阅运行结束
 * - ping: 心跳
 */
class QingLongWebSocketManager(
    private val baseUrl: String,      // 例如 "http://192.168.1.100:5700"
    private val token: String
) {
    companion object {
        private const val TAG = "QingLongWebSocket"
        private const val SOCKJS_PATH = "/api/ws"
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val RECONNECT_INTERVAL_MS = 3000L
        private const val HEARTBEAT_INTERVAL_MS = 30000L
    }

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // 无超时，WebSocket 需要长连接
        .writeTimeout(10, TimeUnit.SECONDS)
        .followRedirects(false) // 禁止跟随重定向，防止被 JWT 中间件重定向到登录页
        .followSslRedirects(false)
        .build()

    private var webSocket: WebSocket? = null
    private var sessionId: String = ""
    private var serverId: String = ""
    private var reconnectAttempts = 0
    private var heartbeatJob: Job? = null
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 连接状态
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // 消息回调
    private val messageCallbacks = mutableMapOf<String, MutableList<(SockMessage) -> Unit>>()
    // 所有消息的通用回调（用于调试）
    private val allMessageCallbacks = mutableListOf<(String) -> Unit>()

    /**
     * 注册通用消息回调（所有收到的消息都会触发）
     */
    fun onAnyMessage(callback: (String) -> Unit) {
        allMessageCallbacks.add(callback)
    }

    /**
     * 连接 WebSocket
     */
    suspend fun connect(): Boolean {
        try {
            _connectionState.value = ConnectionState.CONNECTING

            // 1. 获取 SockJS 服务器信息
            // 同时通过 URL 参数和 Authorization header 传递 Token，兼容不同版本的青龙面板
            val infoUrl = "${baseUrl}${SOCKJS_PATH}/info?token=$token"
            val infoRequest = Request.Builder()
                .url(infoUrl)
                .header("Authorization", "Bearer $token")
                .build()

            // 使用 withContext(Dispatchers.IO) 执行阻塞的 HTTP 请求
            val infoResponse = withContext(Dispatchers.IO) {
                client.newCall(infoRequest).execute()
            }
            if (!infoResponse.isSuccessful) {
                _connectionState.value = ConnectionState.DISCONNECTED
                return false
            }

            serverId = extractServerId(infoResponse.request.url.toString())

            // 2. 生成随机 session_id
            sessionId = generateSessionId()

            // 3. 建立 WebSocket 连接
            val wsUrl = "${baseUrl}${SOCKJS_PATH}/${serverId}/${sessionId}/websocket?token=$token"
                .replace("http://", "ws://")
                .replace("https://", "wss://")

            val wsRequest = Request.Builder()
                .url(wsUrl)
                .header("Authorization", "Bearer $token")
                .build()

            val deferred = CompletableDeferred<Boolean>()

            // WebSocket 连接在 IO 线程中执行
            withContext(Dispatchers.IO) {
                client.newWebSocket(wsRequest, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        _connectionState.value = ConnectionState.CONNECTED
                        reconnectAttempts = 0
                        startHeartbeat()
                        deferred.complete(true)
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        handleMessage(text)
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        webSocket.close(1000, null)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        _connectionState.value = ConnectionState.DISCONNECTED
                        stopHeartbeat()
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        _connectionState.value = ConnectionState.DISCONNECTED
                        stopHeartbeat()
                        if (!deferred.isCompleted) {
                            deferred.complete(false)
                        } else {
                            scope.launch {
                                reconnect()
                            }
                        }
                    }
                })
            }

            return deferred.await()
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
            return false
        }
    }

    /**
     * 订阅消息类型
     */
    fun subscribe(type: String, callback: (SockMessage) -> Unit) {
        val callbacks = messageCallbacks.getOrPut(type) { mutableListOf() }
        callbacks.add(callback)
    }

    /**
     * 取消订阅
     */
    fun unsubscribe(type: String, callback: (SockMessage) -> Unit) {
        messageCallbacks[type]?.remove(callback)
    }

    /**
     * 发送消息
     * SockJS WebSocket 消息格式：'a' + JSON 数组 ["message"]
     * 例如：a["{\\"action\\":\\"subscribe\\",\\"topic\\":\\"manuallyRunScript\\"}"]
     */
    fun send(message: String) {
        // SockJS WebSocket 消息格式：'a' + JSON 数组 ["message"]
        val sockJsMessage = "a" + gson.toJson(listOf(message))
        webSocket?.send(sockJsMessage)
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        stopHeartbeat()
        webSocket?.close(1000, "Client closing")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
        scope.cancel()
    }

    /**
     * 重新连接
     */
    private suspend fun reconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            _connectionState.value = ConnectionState.DISCONNECTED
            return
        }
        reconnectAttempts++
        delay(RECONNECT_INTERVAL_MS)
        connect()
    }

    /**
     * 处理接收到的消息
     * SockJS WebSocket 传输协议消息格式：
     * - 'o' : 打开连接（open）
     * - 'h' : 心跳（heartbeat）
     * - 'a' : 数组消息，后面跟 JSON 数组，如 a["msg1","msg2"]
     * - 'c' : 关闭连接（close）
     *
     * 青龙面板的日志消息格式：{"type": "manuallyRunScript", "message": "...", "references": [...]}
     * 通过 SockJS 传输时包裹为：a["{\\"type\\":\\"manuallyRunScript\\",\\"message\\":\\"...\\"}"]
     */
    private fun handleMessage(text: String) {
        if (text.isEmpty()) return

        val type = text[0]
        val content = if (text.length > 1) text.substring(1) else ""

        when (type) {
            'o' -> {
                // 打开连接
                _connectionState.value = ConnectionState.CONNECTED
            }
            'h' -> {
                // 心跳，忽略
            }
            'a' -> {
                // 数组消息，解析 JSON 数组
                try {
                    val messages: List<String> = gson.fromJson(content, Array<String>::class.java).toList()
                    for (msg in messages) {
                        // 触发通用回调（所有消息，用于调试）
                        allMessageCallbacks.forEach { it(msg) }
                        try {
                            val sockMessage = gson.fromJson(msg, SockMessage::class.java)
                            val callbacks = messageCallbacks[sockMessage.type]
                            callbacks?.forEach { it(sockMessage) }
                        } catch (e: Exception) {
                            // 单个消息解析失败，继续处理下一个
                        }
                    }
                } catch (e: Exception) {
                    // 数组解析失败，忽略
                }
            }
            'c' -> {
                // 关闭连接
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    /**
     * 心跳
     * SockJS 协议中，心跳消息是发送 '[]'（空 JSON 数组）
     */
    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                // SockJS 心跳消息：发送空 JSON 数组
                webSocket?.send("[]")
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    /**
     * 从 info URL 中提取 server_id
     * SockJS 的 server_id 是 info 响应 URL 中的一部分
     * 例如：/api/ws/info?t=1234567890 → server_id 从响应中获取
     * 实际上青龙面板的 SockJS 服务端接受任何 server_id
     */
    private fun extractServerId(@Suppress("UNUSED_PARAMETER") url: String): String {
        // 使用时间戳作为 server_id
        return (System.currentTimeMillis() / 1000).toString()
    }

    /**
     * 生成随机 session_id（8位字母数字）
     */
    private fun generateSessionId(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }
}

/**
 * SockJS 消息
 */
data class SockMessage(
    val type: String = "",
    val message: String = "",
    val references: List<Int>? = null
)
