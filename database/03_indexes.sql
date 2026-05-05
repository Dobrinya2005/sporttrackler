-- =====================================================
-- FITNESS TRAINER APP — INDEXES
-- =====================================================

USE FitnessTrainerDB;
GO

-- Users
CREATE INDEX IX_Users_Email      ON Users(Email);
CREATE INDEX IX_Users_RoleId     ON Users(RoleId);

-- RefreshTokens
CREATE INDEX IX_RefreshTokens_UserId ON RefreshTokens(UserId);
CREATE INDEX IX_RefreshTokens_Token  ON RefreshTokens(Token);

-- ClientProfiles
CREATE INDEX IX_ClientProfiles_TrainerId ON ClientProfiles(TrainerId);

-- TrainerInvites
CREATE INDEX IX_TrainerInvites_TrainerId    ON TrainerInvites(TrainerId);
CREATE INDEX IX_TrainerInvites_ClientEmail  ON TrainerInvites(ClientEmail);
CREATE INDEX IX_TrainerInvites_InviteCode   ON TrainerInvites(InviteCode);

-- Measurements
CREATE INDEX IX_Measurements_ClientId       ON Measurements(ClientId);
CREATE INDEX IX_Measurements_MeasuredAt     ON Measurements(ClientId, MeasuredAt DESC);

-- ProgressPhotos
CREATE INDEX IX_ProgressPhotos_ClientId     ON ProgressPhotos(ClientId);
CREATE INDEX IX_ProgressPhotos_PhotoDate    ON ProgressPhotos(ClientId, PhotoDate DESC);

-- FoodProducts
CREATE INDEX IX_FoodProducts_ExternalId     ON FoodProducts(ExternalId);
CREATE INDEX IX_FoodProducts_Name           ON FoodProducts(Name);

-- FoodDiary
CREATE INDEX IX_FoodDiary_ClientDate        ON FoodDiary(ClientId, DiaryDate);
CREATE INDEX IX_FoodDiary_ProductId         ON FoodDiary(ProductId);

-- WorkoutPlans
CREATE INDEX IX_WorkoutPlans_ClientId       ON WorkoutPlans(ClientId);
CREATE INDEX IX_WorkoutPlans_TrainerId      ON WorkoutPlans(TrainerId);

-- WorkoutDays
CREATE INDEX IX_WorkoutDays_PlanId          ON WorkoutDays(PlanId);

-- Exercises
CREATE INDEX IX_Exercises_DayId             ON Exercises(DayId);

-- WorkoutLogs
CREATE INDEX IX_WorkoutLogs_ClientId        ON WorkoutLogs(ClientId);
CREATE INDEX IX_WorkoutLogs_WorkoutDate     ON WorkoutLogs(ClientId, WorkoutDate DESC);

-- Messages
CREATE INDEX IX_Messages_Sender_Receiver    ON Messages(SenderId, ReceiverId);
CREATE INDEX IX_Messages_Receiver_Sender    ON Messages(ReceiverId, SenderId);
CREATE INDEX IX_Messages_SentAt             ON Messages(SentAt DESC);
CREATE INDEX IX_Messages_IsRead             ON Messages(ReceiverId, IsRead) WHERE IsRead = 0;

-- Notifications
CREATE INDEX IX_Notifications_UserId        ON Notifications(UserId, IsRead);
CREATE INDEX IX_Notifications_CreatedAt     ON Notifications(UserId, CreatedAt DESC);

-- FcmTokens
CREATE INDEX IX_FcmTokens_UserId            ON FcmTokens(UserId);
GO
