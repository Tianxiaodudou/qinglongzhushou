package com.qinglong.app.di

import android.content.Context
import com.qinglong.app.data.api.AuthInterceptor
import com.qinglong.app.data.api.LiveLoggingInterceptor
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
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = AuthInterceptor(
            getToken = { authRepository.getToken() }
        )

        val liveLoggingInterceptor = LiveLoggingInterceptor()

        return OkHttpClient.Builder()
            .addInterceptor(liveLoggingInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideQingLongApi(
        okHttpClient: OkHttpClient
    ): QingLongApi {
        // 初始使用占位 URL，登录后通过 LoginViewModel 动态重建
        val placeholderRetrofit = Retrofit.Builder()
            .baseUrl("https://placeholder.local/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return placeholderRetrofit.create(QingLongApi::class.java)
    }

    /**
     * 根据服务器配置动态创建新的 Retrofit 和 QingLongApi 实例
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
