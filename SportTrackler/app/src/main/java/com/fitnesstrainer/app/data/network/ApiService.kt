package com.fitnesstrainer.app.data.network

import com.fitnesstrainer.app.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ─────────────────────────────────────────────────
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<MessageResponse>

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

    @POST("api/plans")
    suspend fun createWorkoutPlan(@Body request: WorkoutPlanCreateRequest): Response<WorkoutPlan>

    @DELETE("api/plans/{planId}")
    suspend fun deleteWorkoutPlan(@Path("planId") planId: Int): Response<Unit>

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

    @Multipart
    @POST("api/messages/upload-media")
    suspend fun uploadChatMedia(
        @Part file: MultipartBody.Part
    ): Response<Map<String, String>>

    // ── FCM ──────────────────────────────────────────────────
    @POST("api/fcm/token")
    suspend fun registerFcmToken(@Body body: Map<String, String>): Response<Unit>

    // ── Email verification ───────────────────────────────────
    @POST("api/auth/send-code")
    suspend fun sendVerificationCode(@Body request: SendCodeRequest): Response<MessageResponse>

    @POST("api/auth/verify-code")
    suspend fun verifyCode(@Body request: VerifyCodeRequest): Response<AuthResponse>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: SendCodeRequest): Response<MessageResponse>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<MessageResponse>

    // ── Client profile ───────────────────────────────────────
    @POST("api/client/profile")
    suspend fun saveClientProfile(@Body request: SaveClientProfileRequest): Response<ClientProfileDto>

    @GET("api/client/profile/{clientId}")
    suspend fun getClientProfile(@Path("clientId") clientId: Int): Response<ClientProfileDto>

    // ── Trainer list & selection ─────────────────────────────
    @GET("api/client/trainers")
    suspend fun getTrainerList(): Response<List<TrainerListItem>>

    @POST("api/client/select-trainer/{trainerId}")
    suspend fun selectTrainer(@Path("trainerId") trainerId: Int): Response<MessageResponse>

    @POST("api/client/select-trainer-by-code/{code}")
    suspend fun selectTrainerByCode(@Path("code") code: String): Response<MessageResponse>

    @GET("api/trainer/my-code")
    suspend fun getMyTrainerCode(): Response<TrainerCodeResponse>

    // ── Reviews ──────────────────────────────────────────────
    @POST("api/reviews")
    suspend fun addReview(@Body request: AddReviewRequest): Response<MessageResponse>

    @GET("api/reviews/trainer/{trainerId}")
    suspend fun getTrainerReviews(@Path("trainerId") trainerId: Int): Response<ReviewsResponse>

    // ── User profile & avatar ────────────────────────────────
    @GET("api/user/me")
    suspend fun getMe(): Response<UserProfile>

    @Multipart
    @POST("api/user/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): Response<Map<String, String>>

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

    // ── Admin ─────────────────────────────────────────────────
    @GET("api/admin/trainers")
    suspend fun adminGetTrainers(): Response<List<AdminTrainerItem>>

    @POST("api/admin/trainers")
    suspend fun adminCreateTrainer(@Body req: CreateTrainerRequest): Response<MessageResponse>

    @PUT("api/admin/trainers/{id}/block")
    suspend fun adminToggleBlock(@Path("id") id: Int): Response<BlockResponse>

    @DELETE("api/admin/trainers/{id}")
    suspend fun adminDeleteTrainer(@Path("id") id: Int): Response<MessageResponse>

    @GET("api/admin/trainers/{id}/reviews")
    suspend fun adminGetReviews(@Path("id") id: Int): Response<List<AdminReviewItem>>

    @DELETE("api/admin/reviews/{id}")
    suspend fun adminDeleteReview(@Path("id") id: Int): Response<MessageResponse>

    @POST("api/admin/reviews/{id}/reply")
    suspend fun adminReplyReview(@Path("id") id: Int, @Body req: ReplyRequest): Response<MessageResponse>
}
