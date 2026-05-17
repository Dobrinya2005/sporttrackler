package com.fitnesstrainer.app.data.network

import com.fitnesstrainer.app.BuildConfig
import com.fitnesstrainer.app.data.local.TokenStorage
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

object RetrofitClient {

    fun create(tokenStorage: TokenStorage): ApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                // Добавляем JWT токен к каждому запросу
                val token = runBlocking { tokenStorage.getAccessToken() }
                val request = if (token != null) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.BODY
                else
                    HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Доверяем self-signed сертификату для локальной разработки
            .apply { trustAllCertificates() }
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    // Только для DEV — принимает любой SSL сертификат
    private fun OkHttpClient.Builder.trustAllCertificates(): OkHttpClient.Builder {
        if (!BuildConfig.DEBUG) return this
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, arrayOf(trustManager), SecureRandom())
        }
        sslSocketFactory(sslContext.socketFactory, trustManager)
        hostnameVerifier { _, _ -> true }
        return this
    }
}
