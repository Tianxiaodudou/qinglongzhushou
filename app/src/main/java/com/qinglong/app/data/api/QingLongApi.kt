package com.qinglong.app.data.api

import com.qinglong.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface QingLongApi {

    // ===================== Auth =====================

    @POST("api/user/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    /**
     * 两步验证 - 登录验证
     * PUT /api/user/two-factor/login
     * Body: {code, username, password}
     */
    @PUT("api/user/two-factor/login")
    suspend fun twoFactorLogin(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<LoginResponse>

    @POST("api/user/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    /**
     * 获取用户信息
     * 官方路径: GET /api/user/（根路径，不是 /api/user/info）
     */
    @GET("api/user/")
    suspend fun getUserInfo(): Response<ApiResponse<UserInfo>>

    data class UserInfo(
        val username: String,
        val avatar: String?
    )

    // ===================== Task =====================

    @GET("api/crons")
    suspend fun getTasks(
        @Query("searchValue") searchValue: String? = null,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
        @Query("filters") filters: String? = null,
        @Query("queryString") queryString: String? = null
    ): Response<ApiResponse<PagedData<Task>>>

    @GET("api/crons/views")
    suspend fun getTaskViews(): Response<ViewsResponse>

    @POST("api/crons/views")
    suspend fun createTaskView(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<ApiResponse<ViewItem>>

    @PUT("api/crons/views")
    suspend fun updateTaskView(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<ApiResponse<ViewItem>>

    @HTTP(method = "DELETE", path = "api/crons/views", hasBody = true)
    suspend fun deleteTaskViews(@Body ids: List<Int>): Response<ApiResponse<Unit>>

    @POST("api/crons")
    suspend fun createTask(@Body body: Map<String, Any>): Response<ApiResponse<Task>>

    @PUT("api/crons")
    suspend fun updateTask(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<ApiResponse<Task>>

    @HTTP(method = "DELETE", path = "api/crons", hasBody = true)
    suspend fun deleteTasks(@Body ids: List<Int>): Response<ApiResponse<Unit>>

    @PUT("api/crons/run")
    suspend fun runTasks(@Body ids: List<Int>): Response<ApiResponse<Unit>>

    @PUT("api/crons/stop")
    suspend fun stopTasks(@Body ids: List<Int>): Response<ApiResponse<Unit>>

    @PUT("api/crons/enable")
    suspend fun enableTasks(@Body ids: List<Int>): Response<ApiResponse<Unit>>

    @PUT("api/crons/disable")
    suspend fun disableTasks(@Body ids: List<Int>): Response<ApiResponse<Unit>>

    @PUT("api/crons/pin")
    suspend fun pinTasks(@Body ids: List<Int>): Response<ApiResponse<Unit>>

    @PUT("api/crons/unpin")
    suspend fun unpinTasks(@Body ids: List<Int>): Response<ApiResponse<Unit>>

    // ===================== Subscription =====================

    /**
     * 获取订阅列表
     * 官方参数名: searchValue（不是 search）
     */
    @GET("api/subscriptions")
    suspend fun getSubscriptions(
        @Query("searchValue") searchValue: String? = null
    ): Response<ApiResponse<List<Subscription>>>

    @POST("api/subscriptions")
    suspend fun createSubscription(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ApiResponse<Subscription>>

    @PUT("api/subscriptions")
    suspend fun updateSubscription(
        @Body body: @JvmSuppressWildcards Map<String, Any>
    ): Response<ApiResponse<Subscription>>

    @HTTP(method = "DELETE", path = "api/subscriptions", hasBody = true)
    suspend fun deleteSubscriptions(
        @Body ids: List<Int>,
        @Query("force") force: Boolean? = null
    ): Response<ApiResponse<Unit>>

    /**
     * 批量运行订阅
     * 官方: PUT /api/subscriptions/run (body: [id])
     * 注意：官方没有单个运行的 API，统一用批量接口传单个 id
     */
    @PUT("api/subscriptions/run")
    suspend fun runSubscriptions(@Body ids: List<Int>): Response<ApiResponse<Unit>>

    /**
     * 批量停止订阅
     * 官方: PUT /api/subscriptions/stop (body: [id])
     */
    @PUT("api/subscriptions/stop")
    suspend fun stopSubscriptions(@Body ids: List<Int>): Response<ApiResponse<Unit>>

    @PUT("api/subscriptions/enable")
    suspend fun enableSubscriptions(@Body ids: List<Int>): Response<ApiResponse<Unit>>

    @PUT("api/subscriptions/disable")
    suspend fun disableSubscriptions(@Body ids: List<Int>): Response<ApiResponse<Unit>>

    /**
     * 获取订阅实时日志
     * 官方: GET /api/subscriptions/:id/log（单数 log，不是复数 logs）
     * 返回 data 是字符串（日志内容）
     */
    @GET("api/subscriptions/{id}/log")
    suspend fun getSubscriptionLog(@Path("id") subId: Int): Response<ApiResponse<String>>

    /**
     * 获取订阅历史日志列表
     * 官方: GET /api/subscriptions/:id/logs（复数 logs）
     * 返回 data 是日志文件列表
     */
    @GET("api/subscriptions/{id}/logs")
    suspend fun getSubscriptionLogFiles(@Path("id") subId: Int): Response<ApiResponse<List<CronLogFile>>>

    // ===================== Log =====================

    /**
     * 获取任务实时日志
     * 官方: GET /api/crons/:id/log
     * 返回 data 是字符串
     */
    @GET("api/crons/{id}/log")
    suspend fun getCronLog(@Path("id") id: Int): Response<ApiResponse<String>>

    /**
     * 获取任务历史日志文件列表
     * 官方: GET /api/crons/:id/logs
     */
    @GET("api/crons/{id}/logs")
    suspend fun getCronLogFiles(@Path("id") id: Int): Response<ApiResponse<List<CronLogFile>>>

    /**
     * 获取日志详情（历史日志文件内容）
     * 官方: GET /api/logs/detail?file=xxx&path=xxx
     * 需要 file 和 path 两个参数
     * 返回 data 是字符串（日志内容），不是对象
     */
    @GET("api/logs/detail")
    suspend fun getLogDetail(
        @Query("file") file: String,
        @Query("path") path: String = ""
    ): Response<ApiResponse<String>>

    /**
     * 删除日志文件
     * 官方: DELETE /api/logs (body: {filename, path, type})
     */
    @HTTP(method = "DELETE", path = "api/logs", hasBody = true)
    suspend fun deleteLogFile(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<ApiResponse<Unit>>

    /**
     * 获取日志目录列表
     * 官方: GET /api/logs/
     */
    @GET("api/logs/")
    suspend fun getLogFiles(): Response<ApiResponse<List<LogFile>>>

    // ===================== Env Variable =====================

    @GET("api/envs")
    suspend fun getEnvVariables(
        @Query("searchValue") searchValue: String? = null,
        @Query("type") type: String? = null
    ): Response<ApiResponse<List<EnvVariable>>>

    @POST("api/envs")
    suspend fun createEnvVariables(@Body body: @JvmSuppressWildcards List<Map<String, Any>>): Response<ApiResponse<List<EnvVariable>>>

    @PUT("api/envs")
    suspend fun updateEnvVariable(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ApiResponse<EnvVariable>>

    @HTTP(method = "DELETE", path = "api/envs", hasBody = true)
    suspend fun deleteEnvVariables(@Body ids: List<Int>): Response<ApiResponse<Unit>>

    @PUT("api/envs/enable")
    suspend fun enableEnvVariables(@Body ids: List<Int>): Response<ApiResponse<Unit>>

    @PUT("api/envs/disable")
    suspend fun disableEnvVariables(@Body ids: List<Int>): Response<ApiResponse<Unit>>

    @PUT("api/envs/pin")
    suspend fun pinEnvVariables(@Body ids: List<Int>): Response<ApiResponse<Unit>>

    @PUT("api/envs/unpin")
    suspend fun unpinEnvVariables(@Body ids: List<Int>): Response<ApiResponse<Unit>>

    // ===================== Script =====================

    /**
     * 获取脚本列表
     * 官方只支持 @Query("path")（指定目录），不支持 search/page/size
     */
    @GET("api/scripts")
    suspend fun getScripts(
        @Query("path") path: String? = null
    ): Response<ApiResponse<List<ScriptItem>>>

    @GET("api/scripts/detail")
    suspend fun getScriptDetail(
        @Query("file") file: String,
        @Query("path") path: String = ""
    ): Response<ApiResponse<String>>

    @POST("api/scripts")
    suspend fun createScript(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ApiResponse<ScriptItem>>

    @PUT("api/scripts")
    suspend fun updateScript(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ApiResponse<ScriptItem>>

    @HTTP(method = "DELETE", path = "api/scripts", hasBody = true)
    suspend fun deleteScripts(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ApiResponse<Unit>>

    @PUT("api/scripts/run")
    suspend fun runScript(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ApiResponse<Int>>

    @PUT("api/scripts/stop")
    suspend fun stopScript(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ApiResponse<Unit>>

    @PUT("api/scripts/rename")
    suspend fun renameScript(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ApiResponse<Unit>>

    // ===================== Config =====================

    /**
     * 获取配置文件列表
     * GET /api/configs/files
     * 返回: {code:200, data:[{title: "config.sh", value: "config.sh"}, ...]}
     */
    @GET("api/configs/files")
    suspend fun getConfigFiles(): Response<ApiResponse<List<ConfigFileItem>>>

    /**
     * 获取配置文件内容
     * GET /api/configs/detail?path=xxx
     * 返回: {code:200, data: "文件内容字符串"}
     */
    @GET("api/configs/detail")
    suspend fun getConfigDetail(@Query("path") path: String): Response<ApiResponse<String>>

    /**
     * 保存配置文件
     * POST /api/configs/save
     * Body: {name: "config.sh", content: "文件内容"}
     * 返回: {code:200, message: "保存成功"}
     */
    @POST("api/configs/save")
    suspend fun saveConfig(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ApiResponse<Unit>>

    // ===================== User / Security =====================

    /**
     * 更新用户名密码
     * PUT /api/user
     * Body: {username, password}
     */
    @PUT("api/user")
    suspend fun updateUser(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ApiResponse<Unit>>

    /**
     * 两步验证 - 初始化
     * GET /api/user/two-factor/init
     */
    @GET("api/user/two-factor/init")
    suspend fun getTwoFactorInit(): Response<ApiResponse<Map<String, @JvmSuppressWildcards Any>>>

    /**
     * 两步验证 - 激活
     * PUT /api/user/two-factor/active
     */
    @PUT("api/user/two-factor/active")
    suspend fun activateTwoFactor(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ApiResponse<Boolean>>

    /**
     * 两步验证 - 停用
     * PUT /api/user/two-factor/deactive
     */
    @PUT("api/user/two-factor/deactive")
    suspend fun deactivateTwoFactor(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ApiResponse<Boolean>>

    /**
     * 获取当前用户 twoFactorActivated 状态
     * 通过 GET /api/user/ 获取
     */
    // 复用已有的 getUserInfo()

    // ===================== Open Platform / App Management =====================

    /**
     * 获取应用列表
     * GET /api/apps
     */
    @GET("api/apps")
    suspend fun getApps(): Response<ApiResponse<List<Map<String, @JvmSuppressWildcards Any>>>>

    /**
     * 创建应用
     * POST /api/apps
     * Body: {name, scopes: ["crons", "envs"]}
     */
    @POST("api/apps")
    suspend fun createApp(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ApiResponse<Map<String, @JvmSuppressWildcards Any>>>

    /**
     * 更新应用
     * PUT /api/apps
     * Body: {id, name, scopes}
     */
    @PUT("api/apps")
    suspend fun updateApp(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ApiResponse<Map<String, @JvmSuppressWildcards Any>>>

    /**
     * 删除应用
     * DELETE /api/apps
     * Body: [id]
     */
    @HTTP(method = "DELETE", path = "api/apps", hasBody = true)
    suspend fun deleteApps(@Body ids: List<Int>): Response<ApiResponse<Unit>>

    /**
     * 重置应用密钥
     * PUT /api/apps/{id}/reset-secret
     */
    @PUT("api/apps/{id}/reset-secret")
    suspend fun resetAppSecret(@Path("id") id: Int): Response<ApiResponse<Map<String, @JvmSuppressWildcards Any>>>

    // ===================== System Config =====================

    /**
     * 获取系统配置
     * GET /api/system/config
     */
    @GET("api/system/config")
    suspend fun getSystemConfig(): Response<ApiResponse<Map<String, @JvmSuppressWildcards Any>>>

    /**
     * 更新系统配置（依赖代理、镜像源等）
     * PUT /api/system/config/xxx
     */
    @PUT("api/system/config/{path}")
    suspend fun updateSystemConfig(
        @Path("path") path: String,
        @Body body: @JvmSuppressWildcards Map<String, Any>
    ): Response<ApiResponse<Unit>>

    /**
     * 清除依赖缓存
     * PUT /api/system/config/dependence-clean
     */
    @PUT("api/system/config/dependence-clean")
    suspend fun cleanDependenceCache(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ApiResponse<Unit>>

    /**
     * 删除系统日志
     * DELETE /api/system/log
     */
    @HTTP(method = "DELETE", path = "api/system/log", hasBody = true)
    suspend fun deleteSystemLog(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ApiResponse<Unit>>

    // ===================== Notification =====================

    /**
     * 获取通知设置
     * GET /api/user/notification
     * 返回: {code:200, data: {type: "bark", barkPush: "xxx", ...}}
     */
    @GET("api/user/notification")
    suspend fun getNotificationSettings(): Response<ApiResponse<Map<String, @JvmSuppressWildcards Any>>>

    /**
     * 更新通知设置
     * PUT /api/user/notification
     * Body: {type: "bark", barkPush: "xxx", ...}
     * 返回: {code:200, message: "通知发送成功"}
     * 注意: 青龙会在保存前发送测试通知，测试成功后才保存
     */
    @PUT("api/user/notification")
    suspend fun updateNotificationSettings(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ApiResponse<Unit>>

    // ===================== System =====================

    @GET("api/system")
    suspend fun getSystemStatus(): Response<ApiResponse<SystemStatus>>

    @GET("api/system")
    suspend fun getSystemVersion(): Response<ApiResponse<Map<String, @JvmSuppressWildcards Any>>>

    /**
     * 获取系统日志
     * GET /api/system/log?startTime=&endTime=
     * 返回: Blob（纯文本日志内容）
     */
    @GET("api/system/log")
    suspend fun getSystemLog(
        @Query("startTime") startTime: String = "",
        @Query("endTime") endTime: String = ""
    ): Response<okhttp3.ResponseBody>

    // ===================== User / Login Log =====================

    /**
     * 获取登录日志
     * GET /api/user/login-log
     * 返回: {code:200, data: [{id, timestamp, address, ip, platform, status}]}
     */
    @GET("api/user/login-log")
    suspend fun getLoginLog(): Response<ApiResponse<List<LoginLogEntry>>>

    // ===================== Dependence =====================

    /**
     * 获取依赖列表
     * GET /api/dependencies?searchValue=&type=&status=
     */
    @GET("api/dependencies")
    suspend fun getDependencies(
        @Query("searchValue") searchValue: String? = null,
        @Query("type") type: String? = null,
        @Query("status") status: String? = null
    ): Response<ApiResponse<List<Dependence>>>

    /**
     * 创建依赖
     * POST /api/dependencies
     * body: [{name, type, remark}]
     */
    @POST("api/dependencies")
    suspend fun createDependencies(@Body body: @JvmSuppressWildcards List<Map<String, Any>>): Response<ApiResponse<List<Dependence>>>

    /**
     * 更新依赖
     * PUT /api/dependencies
     * body: {id, name, type, remark}
     */
    @PUT("api/dependencies")
    suspend fun updateDependence(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ApiResponse<Dependence>>

    /**
     * 删除依赖
     * DELETE /api/dependencies
     * body: [id]
     */
    @HTTP(method = "DELETE", path = "api/dependencies", hasBody = true)
    suspend fun deleteDependencies(@Body ids: List<Int>): Response<ApiResponse<Unit>>

    /**
     * 强制删除依赖
     * DELETE /api/dependencies/force
     * body: [id]
     */
    @HTTP(method = "DELETE", path = "api/dependencies/force", hasBody = true)
    suspend fun forceDeleteDependencies(@Body ids: List<Int>): Response<ApiResponse<Unit>>

    /**
     * 重新安装依赖
     * PUT /api/dependencies/reinstall
     * body: [id]
     */
    @PUT("api/dependencies/reinstall")
    suspend fun reinstallDependencies(@Body ids: List<Int>): Response<ApiResponse<Unit>>

    /**
     * 取消安装依赖
     * PUT /api/dependencies/cancel
     * body: [id]
     */
    @PUT("api/dependencies/cancel")
    suspend fun cancelDependencies(@Body ids: List<Int>): Response<ApiResponse<Unit>>

    /**
     * 获取依赖详情（含日志）
     * GET /api/dependencies/:id
     */
    @GET("api/dependencies/{id}")
    suspend fun getDependenceDetail(@Path("id") id: Int): Response<ApiResponse<Dependence>>
}
