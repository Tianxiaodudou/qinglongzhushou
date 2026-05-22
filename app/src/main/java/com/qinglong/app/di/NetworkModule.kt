package com.qinglong.app.di

import android.content.Context
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
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = AuthInterceptor(
            getToken = { authRepository.getToken() }
        )

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        authRepository: AuthRepository
    ): Retrofit {
        val server = authRepository.currentServer.value
        val baseUrl = if (server != null) {
            "${server.protocol}://${server.domain}:${server.port}/"
        } else {
            "https://localhost/"
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideQingLongApi(retrofit: Retrofit): QingLongApi {
        return retrofit.create(QingLongApi::class.java)
    }
}
