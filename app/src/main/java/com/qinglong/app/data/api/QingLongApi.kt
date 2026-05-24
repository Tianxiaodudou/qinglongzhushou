package com.qinglong.app.data.api

import com.qinglong.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface QingLongApi {

    // ===================== Auth =====================

    @POST("api/user/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/user/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    @GET("api/user/info")
    suspend fun getUserInfo(): Response<ApiResponse<UserInfo>>

    data class UserInfo(
        val username: String,
        val avatar: String?
    )

    // ===================== Task =====================

    /**
     * 获取任务列表（分页）
     * 匹配青龙面板 v2.20.2 的实际 API
     * 
     * @param searchValue 搜索关键字
     * @param queryString 查询条件 JSON，如 {"filters":[...],"sorts":null,"filterRelation":"and"}
     */
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
    suspend fun updateTask(@Body body: Map<String, Any>): Response<ApiResponse<Task>>

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

    // ===================== Subscription =====================

    @GET("api/subscriptions")
    suspend fun getSubscriptions(
        @Query("search") search: String? = null
    ): Response<ApiResponse<List<Subscription>>>

    @POST("api/subscriptions")
    suspend fun createSubscription(@Body body: Map<String, Any>): Response<ApiResponse<Subscription>>

    @PUT("api/subscriptions/{id}")
    suspend fun updateSubscription(
        @Path("id") id: Int,
        @Body body: Map<String, Any>
    ): Response<ApiResponse<Subscription>>

    @DELETE("api/subscriptions/{id}")
    suspend fun deleteSubscription(@Path("id") id: Int): Response<ApiResponse<Unit>>

    @POST("api/subscriptions/{id}/run")
    suspend fun runSubscription(@Path("id") id: Int): Response<ApiResponse<Unit>>

    // ===================== Log =====================

    /**
     * 获取指定任务的日志内容（最新一次运行）
     * GET /api/crons/{id}/log
     * 返回: { code: 200, data: "日志文本内容" }
     */
    @GET("api/crons/{id}/log")
    suspend fun getCronLog(@Path("id") id: Int): Response<ApiResponse<String>>

    /**
     * 获取指定任务的历史日志文件列表
     * GET /api/crons/{id}/logs
     * 返回: { code: 200, data: [{ filename, directory, time }] }
     */
    @GET("api/crons/{id}/logs")
    suspend fun getCronLogFiles(@Path("id") id: Int): Response<ApiResponse<List<CronLogFile>>>

    @GET("api/logs")
    suspend fun getLogs(
        @Query("taskId") taskId: String? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): Response<ApiResponse<List<TaskLog>>>

    @GET("api/logs/{id}")
    suspend fun getLogDetail(@Path("id") id: String): Response<ApiResponse<TaskLog>>

    @DELETE("api/logs/{id}")
    suspend fun deleteLog(@Path("id") id: String): Response<ApiResponse<Unit>>

    // ===================== Env Variable =====================

    @GET("api/envs")
    suspend fun getEnvVariables(
        @Query("search") search: String? = null,
        @Query("type") type: String? = null
    ): Response<ApiResponse<List<EnvVariable>>>

    @POST("api/envs")
    suspend fun createEnvVariable(@Body body: Map<String, Any>): Response<ApiResponse<EnvVariable>>

    @PUT("api/envs/{id}")
    suspend fun updateEnvVariable(
        @Path("id") id: String,
        @Body body: Map<String, Any>
    ): Response<ApiResponse<EnvVariable>>

    @DELETE("api/envs/{id}")
    suspend fun deleteEnvVariable(@Path("id") id: String): Response<ApiResponse<Unit>>

    // ===================== System =====================

    @GET("api/system")
    suspend fun getSystemStatus(): Response<ApiResponse<SystemStatus>>
}
