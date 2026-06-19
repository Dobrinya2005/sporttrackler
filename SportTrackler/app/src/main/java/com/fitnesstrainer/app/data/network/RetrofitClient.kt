package com.fitnesstrainer.app.data.network

import com.fitnesstrainer.app.BuildConfig
import com.fitnesstrainer.app.data.local.TokenStorage
import com.fitnesstrainer.app.data.model.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

object RetrofitClient {

    var onSessionExpired: (() -> Unit)? = null
    @Volatile private var sessionExpiredHandled = false

    fun create(tokenStorage: TokenStorage): ApiService {
        val authenticator = object : Authenticator {
            override fun authenticate(route: Route?, response: Response): Request? {
                return try {
                    val url = response.request.url.encodedPath
                    if (url.contains("/auth/") || url.contains("/fcm/")) return null
                    if (response.request.header("X-No-Retry") != null) return null
                    val refreshToken = runBlocking { tokenStorage.getRefreshToken() } ?: run {
                        triggerSessionExpired(); return null
                    }
                    val refreshResponse = runBlocking {
                        try {
                            val tempApi = buildRetrofit(buildBaseClient().build()).create(ApiService::class.java)
                            tempApi.refreshToken(RefreshTokenRequest(refreshToken))
                        } catch (_: Exception) { null }
                    }
                    if (refreshResponse?.isSuccessful == true) {
                        val body = refreshResponse.body()!!
                        runBlocking { tokenStorage.saveAuth(
                            body.accessToken, body.refreshToken,
                            tokenStorage.getUserId(), tokenStorage.getUserRole() ?: "",
                            tokenStorage.getFirstName() ?: "", tokenStorage.getLastName() ?: "",
                            tokenStorage.getEmail() ?: "", tokenStorage.getAvatarUrl()
                        ) }
                        response.request.newBuilder()
                            .header("Authorization", "Bearer ${body.accessToken}")
                            .build()
                    } else {
                        runBlocking { tokenStorage.clearAuth() }
                        triggerSessionExpired()
                        null
                    }
                } catch (_: Exception) { null }
            }
        }

        val client = buildBaseClient()
            .authenticator(authenticator)
            .addInterceptor { chain ->
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
            .apply { trustAllCertificates() }
            .build()

        return buildRetrofit(client).create(ApiService::class.java)
    }

    private fun triggerSessionExpired() {
        if (sessionExpiredHandled) return
        synchronized(this) {
            if (sessionExpiredHandled) return
            sessionExpiredHandled = true
        }
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onSessionExpired?.invoke()
        }
    }

    fun resetSessionExpiredFlag() {
        sessionExpiredHandled = false
    }

    private fun buildBaseClient() = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .connectionPool(ConnectionPool(5, 30, TimeUnit.SECONDS))
        .apply { trustAllCertificates() }

    private fun buildRetrofit(client: OkHttpClient) = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

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
