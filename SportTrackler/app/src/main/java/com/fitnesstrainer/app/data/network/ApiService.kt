package com.fitnesstrainer.app.data.network

import com.fitnesstrainer.app.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ─────────────────────────────────────────────────
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<TokenResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>

    // ── Trainer / Client ─────────────────────────────────────
    @GET("api/trainer/clients")
    suspend fun getMyClients(): Response<List<ClientProfile>>

    @GET("api/client/trainer")
    suspend fun getMyTrainer(): Response<TrainerInfo>

    // ── Measurements ─────────────────────────────────────────
    @POST("api/measurements")
    suspend fun addMeasurement(@Body request: MeasurementRequest): Response<MeasurementResponse>

    @GET("api/measurements/{clientId}")
    suspend fun getMeasurementHistory(
        @Path("clientId") clientId: Int,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<List<MeasurementResponse>>

    @GET("api/measurements/{clientId}/latest")
    suspend fun getLatestMeasurement(@Path("clientId") clientId: Int): Response<MeasurementResponse>

    @DELETE("api/measurements/{measurementId}")
    suspend fun deleteMeasurement(@Path("measurementId") id: Int): Response<Unit>

    // ── Food ─────────────────────────────────────────────────
    @GET("api/food/search")
    suspend fun searchFood(@Query("q") query: String): Response<List<FoodProduct>>

    @POST("api/food/diary")
    suspend fun addDiaryEntry(@Body request: DiaryEntryRequest): Response<DiaryEntryResponse>

    @GET("api/food/diary/{clientId}/{date}")
    suspend fun getDailySummary(
        @Path("clientId") clientId: Int,
        @Path("date") date: String
    ): Response<DailySummaryResponse>

    @DELETE("api/food/diary/{diaryId}")
    suspend fun deleteDiaryEntry(@Path("diaryId") diaryId: Int): Response<Unit>

    // ── Workout ──────────────────────────────────────────────
    @GET("api/plans/{clientId}")
    suspend fun getWorkoutPlans(@Path("clientId") clientId: Int): Response<List<WorkoutPlan>>

    // ── Chat ─────────────────────────────────────────────────
    @GET("api/messages/conversations")
    suspend fun getConversations(): Response<List<ConversationPreview>>

    @GET("api/messages/{contactId}")
    suspend fun getMessages(
        @Path("contactId") contactId: Int,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): Response<List<MessageDto>>

    @POST("api/messages/send")
    suspend fun sendMessage(@Body request: SendMessageRequest): Response<MessageDto>

    @POST("api/messages/{senderId}/read")
    suspend fun markRead(@Path("senderId") senderId: Int): Response<Unit>

    // ── FCM ──────────────────────────────────────────────────
    @POST("api/fcm/token")
    suspend fun registerFcmToken(@Body body: Map<String, String>): Response<Unit>

    // ── Photos ───────────────────────────────────────────────
    @Multipart
    @POST("api/photos/upload")
    suspend fun uploadPhoto(
        @Part file: MultipartBody.Part,
        @Part("poseType") poseType: RequestBody?,
        @Part("description") description: RequestBody?,
        @Part("isVisibleToTrainer") isVisibleToTrainer: RequestBody
    ): Response<ProgressPhoto>

    @GET("api/photos/{clientId}")
    suspend fun getPhotos(@Path("clientId") clientId: Int): Response<List<ProgressPhoto>>
}
