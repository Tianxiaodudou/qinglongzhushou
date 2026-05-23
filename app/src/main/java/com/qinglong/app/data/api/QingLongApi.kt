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
        @Query("queryString") queryString: String? = null
    ): Response<ApiResponse<PagedData<Task>>>

    @POST("api/crons")
    suspend fun createTask(@Body body: Map<String, Any>): Response<ApiResponse<Task>>

    @PUT("api/crons/{id}")
    suspend fun updateTask(
        @Path("id") id: String,
        @Body body: Map<String, Any>
    ): Response<ApiResponse<Task>>

    @DELETE("api/crons/{id}")
    suspend fun deleteTask(@Path("id") id: String): Response<ApiResponse<Unit>>

    @POST("api/crons/{id}/run")
    suspend fun runTask(@Path("id") id: String): Response<ApiResponse<Unit>>

    @POST("api/crons/{id}/stop")
    suspend fun stopTask(@Path("id") id: String): Response<ApiResponse<Unit>>

    @POST("api/crons/{ids}/enable")
    suspend fun enableTasks(@Path("ids") ids: String): Response<ApiResponse<Unit>>

    @POST("api/crons/{ids}/disable")
    suspend fun disableTasks(@Path("ids") ids: String): Response<ApiResponse<Unit>>

    // ===================== Subscription =====================

    @GET("api/subs")
    suspend fun getSubscriptions(
        @Query("search") search: String? = null
    ): Response<ApiResponse<List<Subscription>>>

    @POST("api/subs")
    suspend fun createSubscription(@Body body: Map<String, Any>): Response<ApiResponse<Subscription>>

    @PUT("api/subs/{id}")
    suspend fun updateSubscription(
        @Path("id") id: String,
        @Body body: Map<String, Any>
    ): Response<ApiResponse<Subscription>>

    @DELETE("api/subs/{id}")
    suspend fun deleteSubscription(@Path("id") id: String): Response<ApiResponse<Unit>>

    @POST("api/subs/{id}/run")
    suspend fun runSubscription(@Path("id") id: String): Response<ApiResponse<Unit>>

    // ===================== Log =====================

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
