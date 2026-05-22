package com.qinglong.app.data.api

import com.qinglong.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface QingLongApi {

    // ===================== Auth =====================

    @POST("user/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("user/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    @GET("user/info")
    suspend fun getUserInfo(): Response<ApiResponse<UserInfo>>

    data class UserInfo(
        val username: String,
        val avatar: String?
    )

    // ===================== Task =====================

    @GET("crons")
    suspend fun getTasks(
        @Query("search") search: String? = null,
        @Query("filter") filter: String? = null  // running | stopped
    ): Response<ApiResponse<List<Task>>>

    @POST("crons")
    suspend fun createTask(@Body body: Map<String, Any>): Response<ApiResponse<Task>>

    @PUT("crons/{id}")
    suspend fun updateTask(
        @Path("id") id: String,
        @Body body: Map<String, Any>
    ): Response<ApiResponse<Task>>

    @DELETE("crons/{id}")
    suspend fun deleteTask(@Path("id") id: String): Response<ApiResponse<Unit>>

    @POST("crons/{id}/run")
    suspend fun runTask(@Path("id") id: String): Response<ApiResponse<Unit>>

    @POST("crons/{id}/stop")
    suspend fun stopTask(@Path("id") id: String): Response<ApiResponse<Unit>>

    @POST("crons/{ids}/enable")
    suspend fun enableTasks(@Path("ids") ids: String): Response<ApiResponse<Unit>>

    @POST("crons/{ids}/disable")
    suspend fun disableTasks(@Path("ids") ids: String): Response<ApiResponse<Unit>>

    // ===================== Subscription =====================

    @GET("subs")
    suspend fun getSubscriptions(
        @Query("search") search: String? = null
    ): Response<ApiResponse<List<Subscription>>>

    @POST("subs")
    suspend fun createSubscription(@Body body: Map<String, Any>): Response<ApiResponse<Subscription>>

    @PUT("subs/{id}")
    suspend fun updateSubscription(
        @Path("id") id: String,
        @Body body: Map<String, Any>
    ): Response<ApiResponse<Subscription>>

    @DELETE("subs/{id}")
    suspend fun deleteSubscription(@Path("id") id: String): Response<ApiResponse<Unit>>

    @POST("subs/{id}/run")
    suspend fun runSubscription(@Path("id") id: String): Response<ApiResponse<Unit>>

    // ===================== Log =====================

    @GET("logs")
    suspend fun getLogs(
        @Query("taskId") taskId: String? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): Response<ApiResponse<List<TaskLog>>>

    @GET("logs/{id}")
    suspend fun getLogDetail(@Path("id") id: String): Response<ApiResponse<TaskLog>>

    @DELETE("logs/{id}")
    suspend fun deleteLog(@Path("id") id: String): Response<ApiResponse<Unit>>

    // ===================== Env Variable =====================

    @GET("envs")
    suspend fun getEnvVariables(
        @Query("search") search: String? = null,
        @Query("type") type: String? = null
    ): Response<ApiResponse<List<EnvVariable>>>

    @POST("envs")
    suspend fun createEnvVariable(@Body body: Map<String, Any>): Response<ApiResponse<EnvVariable>>

    @PUT("envs/{id}")
    suspend fun updateEnvVariable(
        @Path("id") id: String,
        @Body body: Map<String, Any>
    ): Response<ApiResponse<EnvVariable>>

    @DELETE("envs/{id}")
    suspend fun deleteEnvVariable(@Path("id") id: String): Response<ApiResponse<Unit>>

    // ===================== System =====================

    @GET("system")
    suspend fun getSystemStatus(): Response<ApiResponse<SystemStatus>>
}
