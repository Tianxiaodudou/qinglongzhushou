package com.qinglong.app.di

import android.content.Context
import com.qinglong.app.data.api.ApiManager
import com.qinglong.app.data.api.AuthInterceptor
import com.qinglong.app.data.api.QingLongApi
import com.qinglong.app.data.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        @ApplicationContext context: Context
    ): AuthRepository = AuthRepository(context)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authRepository: AuthRepository
    ): OkHttpClient {
        // NOTE: 不使用 HttpLoggingInterceptor.Level.BODY，因为 LiveLoggingInterceptor 已经做日志
        // Level.BODY 会完整读取响应体导致 Retrofit 反序列化失败且严重拖慢性能
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        val authInterceptor = AuthInterceptor(
            getToken = { authRepository.getToken() }
        )

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 根据服务器配置动态创建 Retrofit 和 QingLongApi 实例
     */
    fun createApi(
        protocol: String,
        domain: String,
        port: Int,
        okHttpClient: OkHttpClient
    ): QingLongApi {
        val baseUrl = "${protocol}://${domain}:${port}/"
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(QingLongApi::class.java)
    }
}
