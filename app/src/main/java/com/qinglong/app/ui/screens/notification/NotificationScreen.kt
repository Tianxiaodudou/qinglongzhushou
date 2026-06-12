package com.qinglong.app.ui.screens.notification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qinglong.app.ui.components.QingLongTopBar

/**
 * 通知设置页面
 * 对应青龙面板网页端 src/pages/setting/notification.tsx
 *
 * 通知方式列表：与青龙面板 backend/data/notify.ts + backend/services/notify.ts 保持一致
 */
data class NotifyModeInfo(
    val key: String,
    val label: String,
    val fields: List<NotifyField>
)

data class NotifyField(
    val key: String,
    val label: String,
    val hint: String,
    val required: Boolean = false,
    val isSelect: Boolean = false,
    val options: List<Pair<String, String>> = emptyList(),
    val isMultiline: Boolean = false
)

private val NOTIFY_MODES = listOf(
    NotifyModeInfo("closed", "已关闭", emptyList()),
    NotifyModeInfo("gotify", "Gotify", listOf(
        NotifyField("gotifyUrl", "Gotify URL", "gotify的url地址，例如 https://push.example.de:8080", required = true),
        NotifyField("gotifyToken", "Token", "gotify的消息应用token码", required = true),
        NotifyField("gotifyPriority", "优先级", "推送消息的优先级")
    )),
    NotifyModeInfo("ntfy", "Ntfy", listOf(
        NotifyField("ntfyUrl", "Ntfy URL", "ntfy的url地址，例如 https://ntfy.sh", required = true),
        NotifyField("ntfyTopic", "Topic", "ntfy应用topic", required = true),
        NotifyField("ntfyPriority", "优先级", "推送消息的优先级"),
        NotifyField("ntfyToken", "Token", "ntfy应用token"),
        NotifyField("ntfyUsername", "用户名", "ntfy应用用户名"),
        NotifyField("ntfyPassword", "密码", "ntfy应用密码"),
        NotifyField("ntfyActions", "用户动作", "ntfy用户动作")
    )),
    NotifyModeInfo("serverChan", "Server酱", listOf(
        NotifyField("serverChanKey", "SENDKEY", "Server酱SENDKEY", required = true)
    )),
    NotifyModeInfo("pushDeer", "PushDeer", listOf(
        NotifyField("pushDeerKey", "Key", "PushDeer的Key", required = true),
        NotifyField("pushDeerUrl", "URL", "PushDeer的自架API endpoint，默认 https://api2.pushdeer.com/message/push")
    )),
    NotifyModeInfo("bark", "Bark", listOf(
        NotifyField("barkPush", "推送地址", "Bark的信息IP/设备码，例如：https://api.day.app/XXXXXXXX", required = true),
        NotifyField("barkIcon", "图标", "BARK推送图标 (需iOS15+)"),
        NotifyField("barkSound", "铃声", "BARK推送铃声"),
        NotifyField("barkGroup", "分组", "BARK推送消息的分组"),
        NotifyField("barkLevel", "时效性", "BARK推送消息的时效性，默认active"),
        NotifyField("barkUrl", "跳转URL", "BARK推送消息的跳转URL"),
        NotifyField("barkArchive", "保存", "BARK是否保存推送消息")
    )),
    NotifyModeInfo("telegramBot", "Telegram机器人", listOf(
        NotifyField("telegramBotToken", "Bot Token", "telegram机器人的token", required = true),
        NotifyField("telegramBotUserId", "用户ID", "telegram用户的id", required = true),
        NotifyField("telegramBotProxyHost", "代理IP", "代理IP"),
        NotifyField("telegramBotProxyPort", "代理端口", "代理端口"),
        NotifyField("telegramBotProxyAuth", "代理认证", "telegram代理配置认证参数 user:password"),
        NotifyField("telegramBotApiHost", "API地址", "telegram api自建的反向代理地址，默认tg官方api")
    )),
    NotifyModeInfo("dingtalkBot", "钉钉机器人", listOf(
        NotifyField("dingtalkBotToken", "Webhook Token", "钉钉机器人webhook token", required = true),
        NotifyField("dingtalkBotSecret", "密钥", "密钥，机器人安全设置页面加签一栏的SEC开头的字符串")
    )),
    NotifyModeInfo("weWorkBot", "企业微信机器人", listOf(
        NotifyField("weWorkBotKey", "Webhook Key", "企业微信机器人的webhook key", required = true),
        NotifyField("weWorkOrigin", "代理地址", "企业微信代理地址")
    )),
    NotifyModeInfo("weWorkApp", "企业微信应用", listOf(
        NotifyField("weWorkAppKey", "配置", "corpid,corpsecret,touser,agentid,消息类型 用英文逗号隔开", required = true),
        NotifyField("weWorkOrigin", "代理地址", "企业微信代理地址")
    )),
    NotifyModeInfo("email", "邮箱", listOf(
        NotifyField("emailService", "邮件服务", "邮件服务商，如 QQ、Gmail 等", required = true),
        NotifyField("emailUser", "邮箱地址", "发件邮箱地址", required = true),
        NotifyField("emailPass", "密码/授权码", "邮箱密码或授权码", required = true),
        NotifyField("emailTo", "收件人", "收件人邮箱地址（多个用英文分号隔开），留空则发给发件人自己")
    )),
    NotifyModeInfo("lark", "飞书机器人", listOf(
        NotifyField("larkKey", "Webhook URL", "飞书机器人的webhook地址或key", required = true),
        NotifyField("larkSecret", "签名密钥", "飞书机器人的签名校验密钥(如有)")
    )),
    NotifyModeInfo("pushPlus", "PushPlus", listOf(
        NotifyField("pushPlusToken", "Token", "您的Token", required = true),
        NotifyField("pushPlusUser", "群组编码", "一对多推送的群组编码"),
        NotifyField("pushplusTemplate", "发送模板", ""),
        NotifyField("pushplusChannel", "发送渠道", ""),
        NotifyField("pushplusWebhook", "Webhook编码", ""),
        NotifyField("pushplusCallbackUrl", "回调地址", "发送结果回调地址"),
        NotifyField("pushplusTo", "好友令牌", "")
    )),
    NotifyModeInfo("iGot", "IGot", listOf(
        NotifyField("iGotPushKey", "推送Key", "iGot的信息推送key", required = true)
    )),
    NotifyModeInfo("wxPusherBot", "wxPusher", listOf(
        NotifyField("wxPusherBotAppToken", "AppToken", "wxPusherBot的appToken", required = true),
        NotifyField("wxPusherBotTopicIds", "TopicIds", "wxPusherBot的topicIds（多个用英文分号隔开）"),
        NotifyField("wxPusherBotUids", "Uids", "wxPusherBot的uids（多个用英文分号隔开）")
    )),
    NotifyModeInfo("webhook", "自定义通知", listOf(
        NotifyField("webhookUrl", "URL", "Webhook URL，必须包含 ${'$'}title", required = true),
        NotifyField("webhookMethod", "请求方法", "请求方法", isSelect = true,
            options = listOf("GET" to "GET", "POST" to "POST", "PUT" to "PUT")),
        NotifyField("webhookHeaders", "请求头", "请求头，一行一个 Header：Key: Value"),
        NotifyField("webhookBody", "请求体", "请求体，${'$'}title 和 ${'$'}content 会被替换"),
        NotifyField("webhookContentType", "Content-Type", "请求体类型", isSelect = true,
            options = listOf(
                "application/json" to "application/json",
                "application/x-www-form-urlencoded" to "x-www-form-urlencoded",
                "text/plain" to "text/plain"
            ))
    )),
    NotifyModeInfo("chat", "群晖Chat", listOf(
        NotifyField("synologyChatUrl", "Webhook URL", "synologyChat的url地址", required = true)
    )),
    NotifyModeInfo("pushMe", "PushMe", listOf(
        NotifyField("pushMeKey", "Push Key", "PushMe的push_key", required = true),
        NotifyField("pushMeUrl", "URL", "PushMe的URL，默认 https://push.i-i.me/")
    )),
    NotifyModeInfo("goCqHttpBot", "GoCqHttpBot", listOf(
        NotifyField("goCqHttpBotUrl", "URL", "推送到个人QQ或群的URL", required = true),
        NotifyField("goCqHttpBotToken", "Token", "访问密钥", required = true),
        NotifyField("goCqHttpBotQq", "QQ/群ID", "QQ号或群ID参数", required = true)
    )),
    NotifyModeInfo("aibotk", "智能微秘书", listOf(
        NotifyField("aibotkKey", "API Key", "智能微秘书apikey", required = true),
        NotifyField("aibotkType", "发送目标", "发送目标：群聊或好友", isSelect = true, required = true,
            options = listOf("room" to "群聊", "contact" to "好友")),
        NotifyField("aibotkName", "名称", "用户昵称或群名", required = true)
    )),
    NotifyModeInfo("wePlusBot", "微加机器人", listOf(
        NotifyField("wePlusBotToken", "Token", "用户令牌", required = true),
        NotifyField("wePlusBotReceiver", "接收人", "消息接收人"),
        NotifyField("wePlusBotVersion", "版本", "专业版填pro，个人版填personal", isSelect = true,
            options = listOf("pro" to "专业版", "personal" to "个人版"))
    )),
    NotifyModeInfo("chronocat", "Chronocat", listOf(
        NotifyField("chronocatURL", "URL", "Chronocat的URL", required = true),
        NotifyField("chronocatQQ", "QQ/群", "user_id=xxx 或 group_id=xxx", required = true),
        NotifyField("chronocatToken", "Token", "Chronocat的Token", required = true)
    )),
    NotifyModeInfo("openiLink", "OpeniLink", listOf(
        NotifyField("openiLinkAppToken", "App Token", "OpeniLink的app_token", required = true),
        NotifyField("openiLinkHubUrl", "Hub地址", "OpeniLink Hub地址，默认为 https://hub.openilink.com"),
        NotifyField("openiLinkContextToken", "Context Token", "用于标识消息会话上下文的context_token")
    ))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onMenuClick: () -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Toast
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    // 获取当前通知方式的字段定义
    val currentMode = remember(uiState.selectedMode) {
        NOTIFY_MODES.find { it.key == uiState.selectedMode } ?: NOTIFY_MODES[0]
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            QingLongTopBar(
                title = "通知设置",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(
                        onClick = { viewModel.saveSettings() },
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "保存")
                        }
                    }
                }
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 通知方式选择
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "通知方式",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(12.dp))

                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = currentMode.label,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    NOTIFY_MODES.forEach { mode ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = when (mode.key) {
                                                            "closed" -> Icons.Default.NotificationsOff
                                                            "bark" -> Icons.Default.NotificationsActive
                                                            "email" -> Icons.Default.Email
                                                            "telegramBot" -> Icons.AutoMirrored.Filled.Send
                                                            "dingtalkBot", "weWorkBot", "weWorkApp" -> Icons.AutoMirrored.Filled.Chat
                                                            "lark" -> Icons.Default.Forum
                                                            "webhook" -> Icons.Default.Code
                                                            "wxPusherBot" -> Icons.Default.PhoneAndroid
                                                            else -> Icons.Default.Notifications
                                                        },
                                                        contentDescription = null,
                                                        modifier = Modifier.size(20.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(Modifier.width(12.dp))
                                                    Text(mode.label)
                                                }
                                            },
                                            onClick = {
                                                viewModel.updateMode(mode.key)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 已关闭提示
                    if (uiState.selectedMode == "closed") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "通知已关闭，青龙面板不会发送任何通知消息。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 配置字段
                    if (currentMode.fields.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "配置参数",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(12.dp))

                                currentMode.fields.forEach { field ->
                                    NotifyConfigField(
                                        field = field,
                                        value = uiState.fieldValues[field.key] ?: "",
                                        onValueChange = { newValue ->
                                            viewModel.updateFieldValue(field.key, newValue)
                                        }
                                    )
                                    if (field != currentMode.fields.last()) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 底部保存按钮
                    Button(
                        onClick = { viewModel.saveSettings() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !uiState.isSaving,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            if (uiState.isSaving) "保存中..." else "保存通知设置"
                        )
                    }

                    // 提示信息
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "保存时会自动发送测试通知以验证配置是否正确。\n" +
                                    "如果测试失败，配置将不会被保存。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )

        // 错误提示
        if (uiState.error != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = uiState.error ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = { viewModel.loadSettings() }) {
                        Text("重试")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotifyConfigField(
    field: NotifyField,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        // 字段标签
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = field.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (field.required) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "*",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        if (field.isSelect) {
            // 下拉选择器
            var expanded by remember { mutableStateOf(false) }
            val selectedOption = field.options.find { it.first == value }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedOption?.second ?: value,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    field.options.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onValueChange(key)
                                expanded = false
                            }
                        )
                    }
                }
            }
        } else {
            // 文本输入框
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = !field.isMultiline,
                minLines = if (field.isMultiline) 3 else 1,
                maxLines = if (field.isMultiline) 5 else 1,
                placeholder = {
                    if (field.hint.isNotBlank()) {
                        Text(
                            text = field.hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                },
                shape = RoundedCornerShape(8.dp)
            )
        }

        // 提示文字
        if (field.hint.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = field.hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
