package com.fitnesstrainer.app.data.model

import com.google.gson.annotations.SerializedName

// ── Auth ─────────────────────────────────────────────────────

data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val role: String,
    val phone: String?,
    val trainerCode: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val userId: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String,
    val avatarUrl: String?,
    val accessToken: String,
    val refreshToken: String
)

data class RefreshTokenRequest(val refreshToken: String)

data class SendCodeRequest(val email: String)
data class VerifyCodeRequest(val email: String, val code: String)
data class ResetPasswordRequest(val email: String, val code: String, val newPassword: String)
data class MessageResponse(val message: String, val devCode: String? = null)

data class SaveClientProfileRequest(
    val fitnessGoal: String?,
    val activityLevel: String?,
    val weightKg: Double?,
    val heightCm: Double?,
    val goalWeightKg: Double?,
    val gender: String?
)

data class ClientProfileDto(
    val userId: Int,
    val fitnessGoal: String?,
    val activityLevel: String?,
    val weightKg: Double?,
    val heightCm: Double?,
    val goalWeightKg: Double?,
    val birthDate: String?,
    val gender: String?
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String
)

// ── Measurement ──────────────────────────────────────────────

data class MeasurementRequest(
    val measuredAt: String? = null,
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val hipsCm: Double? = null,
    val neckCm: Double? = null,
    val bicepCm: Double? = null,
    val forearmCm: Double? = null,
    val thighCm: Double? = null,
    val calfCm: Double? = null,
    val bodyFatPercent: Double? = null,
    val muscleMassKg: Double? = null,
    val notes: String? = null
)

data class MeasurementResponse(
    val measurementId: Int,
    val clientId: Int,
    val measuredAt: String,
    val weightKg: Double?,
    val heightCm: Double?,
    val chestCm: Double?,
    val waistCm: Double?,
    val hipsCm: Double?,
    val neckCm: Double?,
    val bicepCm: Double?,
    val forearmCm: Double?,
    val thighCm: Double?,
    val calfCm: Double?,
    val bodyFatPercent: Double?,
    val muscleMassKg: Double?,
    val bmi: Double?,
    val notes: String?,
    val createdAt: String
)

// ── Food ─────────────────────────────────────────────────────

data class FoodProduct(
    val productId: Int,
    val externalId: String?,
    val name: String,
    val brand: String?,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val carbsPer100g: Double,
    val fiberPer100g: Double?,
    val isCustom: Boolean
)

data class DiaryEntryRequest(
    val diaryDate: String? = null,
    val mealType: String,
    val productId: Int,
    val weightG: Double
)

data class DiaryEntryResponse(
    val diaryId: Int,
    val diaryDate: String,
    val mealType: String,
    val product: FoodProduct,
    val weightG: Double,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double
)

data class DailySummaryResponse(
    val date: String,
    val calorieGoal: Int?,
    val proteinGoal: Double?,
    val fatGoal: Double?,
    val carbGoal: Double?,
    val totalCalories: Double,
    val totalProtein: Double,
    val totalFat: Double,
    val totalCarbs: Double,
    val meals: List<MealSummary>
)

data class MealSummary(
    val mealType: String,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val items: List<DiaryEntryResponse>
)

// ── Workout ──────────────────────────────────────────────────

data class WorkoutPlanCreateRequest(
    val clientId: Int,
    val title: String,
    val description: String?,
    val startDate: String?,
    val endDate: String?,
    val days: List<WorkoutDayRequest>
)

data class WorkoutDayRequest(
    val dayNumber: Int,
    val dayName: String?,
    val notes: String?,
    val exercises: List<ExerciseRequest>
)

data class ExerciseRequest(
    val orderIndex: Int,
    val name: String,
    val muscleGroup: String?,
    val sets: Int?,
    val reps: String?,
    val weightKg: Double?,
    val restSeconds: Int?,
    val videoUrl: String?,
    val notes: String?
)

data class WorkoutPlan(
    val planId: Int,
    val trainerId: Int,
    val clientId: Int,
    val title: String,
    val description: String?,
    val startDate: String?,
    val endDate: String?,
    val isActive: Boolean,
    val days: List<WorkoutDay>
)

data class WorkoutDay(
    val dayId: Int,
    val dayNumber: Int,
    val dayName: String?,
    val exercises: List<Exercise>
)

data class Exercise(
    val exerciseId: Int,
    val orderIndex: Int,
    val name: String,
    val muscleGroup: String?,
    val sets: Int?,
    val reps: String?,
    val weightKg: Double?,
    val restSeconds: Int?,
    val videoUrl: String?,
    val notes: String?
)

// ── Chat ─────────────────────────────────────────────────────

data class MessageDto(
    val messageId: Int,
    val senderId: Int,
    val senderName: String,
    val senderAvatar: String?,
    val receiverId: Int,
    val messageText: String?,
    val attachmentUrl: String?,
    val attachmentType: String?,
    val isRead: Boolean,
    val sentAt: String
)

data class ConversationPreview(
    val contactId: Int,
    val contactName: String,
    val contactAvatar: String?,
    val lastMessage: String?,
    val lastMessageAt: String,
    val unreadCount: Int
)

data class SendMessageRequest(
    val receiverId: Int,
    val text: String?,
    val attachmentUrl: String? = null,
    val attachmentType: String? = null
)

// ── Group chat ───────────────────────────────────────────────

data class GroupMemberDto(
    val userId: Int,
    val name: String,
    val avatar: String?,
    val isAdmin: Boolean
)

data class GroupDto(
    val groupId: Int,
    val name: String,
    val avatarUrl: String?,
    val createdBy: Int,
    val members: List<GroupMemberDto>,
    val lastMessage: String?,
    val lastMessageAt: String?,
    val unreadCount: Int
)

data class GroupMessageDto(
    val messageId: Int,
    val groupId: Int,
    val senderId: Int,
    val senderName: String,
    val senderAvatar: String?,
    val messageText: String?,
    val attachmentUrl: String?,
    val attachmentType: String?,
    val sentAt: String
)

data class CreateGroupRequest(
    val name: String,
    val memberIds: List<Int>
)

data class SendGroupMessageRequest(
    val text: String?,
    val attachmentUrl: String? = null,
    val attachmentType: String? = null
)

// ── Trainer / Client ─────────────────────────────────────────

data class ClientProfile(
    val userId: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String?,
    val avatarUrl: String?
)

data class TrainerInfo(
    val userId: Int,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String?
)

data class TrainerCodeResponse(
    val trainerCode: String
)

// ── Photo ─────────────────────────────────────────────────────

data class ProgressPhoto(
    val photoId: Int,
    val photoUrl: String,
    val thumbnailUrl: String?,
    val photoDate: String,
    val poseType: String?,
    val description: String?,
    val isVisibleToTrainer: Boolean,
    val createdAt: String
)

// ── Trainer list (for client to choose) ──────────────────────

data class TrainerListItem(
    val userId: Int,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String?,
    val specialization: String?,
    val experience: Int?,
    val description: String?,
    val rating: Double?,
    val reviewCount: Int
)

// ── Reviews ──────────────────────────────────────────────────

data class AddReviewRequest(
    val trainerId: Int,
    val rating: Int,
    val comment: String?
)

data class ReviewItem(
    val reviewId: Int,
    val clientId: Int,
    val clientName: String,
    val avatarUrl: String?,
    val rating: Int,
    val comment: String?,
    val createdAt: String
)

data class ReviewsResponse(
    val averageRating: Double,
    val reviews: List<ReviewItem>
)

// ── Admin ─────────────────────────────────────────────────────

data class AdminTrainerItem(
    val userId: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String?,
    val avatarUrl: String?,
    val isActive: Boolean,
    val trainerCode: String,
    val createdAt: String,
    val clientCount: Int,
    val reviewCount: Int,
    val avgRating: Double?
)

data class AdminReviewItem(
    val reviewId: Int,
    val clientName: String,
    val avatarUrl: String?,
    val rating: Int,
    val comment: String?,
    val adminReply: String?,
    val adminReplyAt: String?,
    val createdAt: String
)

data class AdminClientItem(
    val userId: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String?,
    val avatarUrl: String?,
    val isActive: Boolean,
    val createdAt: String,
    val trainerName: String?,
    val trainerId: Int?
)

data class CreateTrainerRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String
)

data class ReplyRequest(val reply: String)

data class UpdateProfileRequest(
    val firstName: String,
    val lastName: String,
    val phone: String?
)

data class UpdateProfileResponse(
    val firstName: String,
    val lastName: String,
    val phone: String?
)

data class BlockResponse(val isActive: Boolean)

// ── User profile ─────────────────────────────────────────────

data class UserProfile(
    val userId: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String?,
    val role: String,
    val avatarUrl: String?
)
