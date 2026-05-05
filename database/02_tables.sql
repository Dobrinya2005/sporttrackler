
USE FitnessTrainerDB;
GO

-- ─────────────────────────────────────────────────────
-- 1. ROLES — роли пользователей
-- ─────────────────────────────────────────────────────
CREATE TABLE Roles (
    RoleId      INT           PRIMARY KEY IDENTITY(1,1),
    RoleName    NVARCHAR(50)  NOT NULL UNIQUE  -- 'Trainer', 'Client'
);
GO

INSERT INTO Roles (RoleName) VALUES ('Trainer'), ('Client');
GO

-- ─────────────────────────────────────────────────────
-- 2. USERS — учётные записи
-- ─────────────────────────────────────────────────────
CREATE TABLE Users (
    UserId          INT             PRIMARY KEY IDENTITY(1,1),
    RoleId          INT             NOT NULL,
    FirstName       NVARCHAR(100)   NOT NULL,
    LastName        NVARCHAR(100)   NOT NULL,
    Email           NVARCHAR(255)   NOT NULL UNIQUE,
    PasswordHash    NVARCHAR(512)   NOT NULL,
    Phone           NVARCHAR(20)    NULL,
    AvatarUrl       NVARCHAR(500)   NULL,
    IsActive        BIT             NOT NULL DEFAULT 1,
    CreatedAt       DATETIME2       NOT NULL DEFAULT GETUTCDATE(),
    UpdatedAt       DATETIME2       NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT FK_Users_Roles FOREIGN KEY (RoleId) REFERENCES Roles(RoleId)
);
GO

-- ─────────────────────────────────────────────────────
-- 3. REFRESH_TOKENS — токены обновления JWT
-- ─────────────────────────────────────────────────────
CREATE TABLE RefreshTokens (
    TokenId         INT             PRIMARY KEY IDENTITY(1,1),
    UserId          INT             NOT NULL,
    Token           NVARCHAR(512)   NOT NULL UNIQUE,
    ExpiresAt       DATETIME2       NOT NULL,
    CreatedAt       DATETIME2       NOT NULL DEFAULT GETUTCDATE(),
    IsRevoked       BIT             NOT NULL DEFAULT 0,

    CONSTRAINT FK_RefreshTokens_Users FOREIGN KEY (UserId) REFERENCES Users(UserId) ON DELETE CASCADE
);
GO

-- ─────────────────────────────────────────────────────
-- 4. TRAINER_PROFILES — профиль тренера
-- ─────────────────────────────────────────────────────
CREATE TABLE TrainerProfiles (
    TrainerProfileId    INT             PRIMARY KEY IDENTITY(1,1),
    UserId              INT             NOT NULL UNIQUE,
    Specialization      NVARCHAR(255)   NULL,
    Experience          INT             NULL,   -- лет опыта
    Description         NVARCHAR(1000)  NULL,
    CertificateUrl      NVARCHAR(500)   NULL,

    CONSTRAINT FK_TrainerProfiles_Users FOREIGN KEY (UserId) REFERENCES Users(UserId) ON DELETE CASCADE
);
GO

-- ─────────────────────────────────────────────────────
-- 5. CLIENT_PROFILES — профиль клиента
-- ─────────────────────────────────────────────────────
CREATE TABLE ClientProfiles (
    ClientProfileId     INT             PRIMARY KEY IDENTITY(1,1),
    UserId              INT             NOT NULL UNIQUE,
    TrainerId           INT             NULL,   -- тренер клиента
    BirthDate           DATE            NULL,
    Gender              NCHAR(1)        NULL CHECK (Gender IN ('M', 'F')),
    HeightCm            DECIMAL(5,1)    NULL,
    InitialWeightKg     DECIMAL(5,2)    NULL,
    GoalWeight          DECIMAL(5,2)    NULL,
    ActivityLevel       NVARCHAR(50)    NULL    -- sedentary, light, moderate, active, very_active
        CHECK (ActivityLevel IN ('sedentary','light','moderate','active','very_active')),
    FitnessGoal         NVARCHAR(100)   NULL,   -- 'Похудение', 'Набор массы', 'Поддержание формы'
    DailyCalorieGoal    INT             NULL,
    DailyProteinGoal    DECIMAL(6,1)    NULL,
    DailyFatGoal        DECIMAL(6,1)    NULL,
    DailyCarbGoal       DECIMAL(6,1)    NULL,
    CreatedAt           DATETIME2       NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT FK_ClientProfiles_Users    FOREIGN KEY (UserId)    REFERENCES Users(UserId)  ON DELETE CASCADE,
    CONSTRAINT FK_ClientProfiles_Trainer  FOREIGN KEY (TrainerId) REFERENCES Users(UserId)
);
GO

-- ─────────────────────────────────────────────────────
-- 6. TRAINER_INVITES — приглашения клиентов тренером
-- ─────────────────────────────────────────────────────
CREATE TABLE TrainerInvites (
    InviteId        INT             PRIMARY KEY IDENTITY(1,1),
    TrainerId       INT             NOT NULL,
    ClientEmail     NVARCHAR(255)   NOT NULL,
    InviteCode      NVARCHAR(100)   NOT NULL UNIQUE,
    Status          NVARCHAR(20)    NOT NULL DEFAULT 'pending'
        CHECK (Status IN ('pending','accepted','declined','expired')),
    ExpiresAt       DATETIME2       NOT NULL,
    CreatedAt       DATETIME2       NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT FK_TrainerInvites_Trainer FOREIGN KEY (TrainerId) REFERENCES Users(UserId)
);
GO

-- ─────────────────────────────────────────────────────
-- 7. MEASUREMENTS — замеры тела клиента
-- ─────────────────────────────────────────────────────
CREATE TABLE Measurements (
    MeasurementId       INT             PRIMARY KEY IDENTITY(1,1),
    ClientId            INT             NOT NULL,
    MeasuredAt          DATE            NOT NULL DEFAULT CAST(GETUTCDATE() AS DATE),
    WeightKg            DECIMAL(5,2)    NULL,
    HeightCm            DECIMAL(5,1)    NULL,
    ChestCm             DECIMAL(5,1)    NULL,
    WaistCm             DECIMAL(5,1)    NULL,
    HipsCm              DECIMAL(5,1)    NULL,
    NeckCm              DECIMAL(5,1)    NULL,
    BicepCm             DECIMAL(5,1)    NULL,
    ForearmCm           DECIMAL(5,1)    NULL,
    ThighCm             DECIMAL(5,1)    NULL,
    CalfCm              DECIMAL(5,1)    NULL,
    BodyFatPercent      DECIMAL(5,2)    NULL,
    MuscleMassKg        DECIMAL(5,2)    NULL,
    BMI                 AS (
        CASE WHEN HeightCm > 0 AND WeightKg > 0
        THEN ROUND(WeightKg / POWER(HeightCm / 100.0, 2), 2)
        ELSE NULL END
    ) PERSISTED,
    Notes               NVARCHAR(500)   NULL,
    CreatedAt           DATETIME2       NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT FK_Measurements_Client FOREIGN KEY (ClientId) REFERENCES Users(UserId) ON DELETE CASCADE
);
GO

-- ─────────────────────────────────────────────────────
-- 8. PROGRESS_PHOTOS — фотографии прогресса
-- ─────────────────────────────────────────────────────
CREATE TABLE ProgressPhotos (
    PhotoId         INT             PRIMARY KEY IDENTITY(1,1),
    ClientId        INT             NOT NULL,
    PhotoUrl        NVARCHAR(500)   NOT NULL,
    ThumbnailUrl    NVARCHAR(500)   NULL,
    PhotoDate       DATE            NOT NULL DEFAULT CAST(GETUTCDATE() AS DATE),
    PoseType        NVARCHAR(50)    NULL,   -- 'front', 'back', 'side_left', 'side_right'
    Description     NVARCHAR(500)   NULL,
    IsVisibleToTrainer BIT          NOT NULL DEFAULT 1,
    CreatedAt       DATETIME2       NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT FK_ProgressPhotos_Client FOREIGN KEY (ClientId) REFERENCES Users(UserId) ON DELETE CASCADE
);
GO

-- ─────────────────────────────────────────────────────
-- 9. FOOD_PRODUCTS — справочник продуктов (кэш из API)
-- ─────────────────────────────────────────────────────
CREATE TABLE FoodProducts (
    ProductId       INT             PRIMARY KEY IDENTITY(1,1),
    ExternalId      NVARCHAR(100)   NULL,       -- ID из Open Food Facts или другого API
    Name            NVARCHAR(255)   NOT NULL,
    Brand           NVARCHAR(255)   NULL,
    CaloriesPer100g DECIMAL(7,2)    NOT NULL DEFAULT 0,
    ProteinPer100g  DECIMAL(6,2)    NOT NULL DEFAULT 0,
    FatPer100g      DECIMAL(6,2)    NOT NULL DEFAULT 0,
    CarbsPer100g    DECIMAL(6,2)    NOT NULL DEFAULT 0,
    FiberPer100g    DECIMAL(6,2)    NULL DEFAULT 0,
    IsCustom        BIT             NOT NULL DEFAULT 0,  -- создан пользователем
    CreatedBy       INT             NULL,
    CreatedAt       DATETIME2       NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT FK_FoodProducts_User FOREIGN KEY (CreatedBy) REFERENCES Users(UserId)
);
GO

-- ─────────────────────────────────────────────────────
-- 10. FOOD_DIARY — дневник питания
-- ─────────────────────────────────────────────────────
CREATE TABLE FoodDiary (
    DiaryId         INT             PRIMARY KEY IDENTITY(1,1),
    ClientId        INT             NOT NULL,
    DiaryDate       DATE            NOT NULL DEFAULT CAST(GETUTCDATE() AS DATE),
    MealType        NVARCHAR(20)    NOT NULL
        CHECK (MealType IN ('breakfast','lunch','dinner','snack')),
    ProductId       INT             NOT NULL,
    WeightG         DECIMAL(7,2)    NOT NULL,
    CreatedAt       DATETIME2       NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT FK_FoodDiary_Client  FOREIGN KEY (ClientId)  REFERENCES Users(UserId)        ON DELETE CASCADE,
    CONSTRAINT FK_FoodDiary_Product FOREIGN KEY (ProductId) REFERENCES FoodProducts(ProductId)
);
GO

-- ─────────────────────────────────────────────────────
-- 11. WORKOUT_PLANS — тренировочные планы
-- ─────────────────────────────────────────────────────
CREATE TABLE WorkoutPlans (
    PlanId          INT             PRIMARY KEY IDENTITY(1,1),
    TrainerId       INT             NOT NULL,
    ClientId        INT             NOT NULL,
    Title           NVARCHAR(255)   NOT NULL,
    Description     NVARCHAR(1000)  NULL,
    StartDate       DATE            NULL,
    EndDate         DATE            NULL,
    IsActive        BIT             NOT NULL DEFAULT 1,
    CreatedAt       DATETIME2       NOT NULL DEFAULT GETUTCDATE(),
    UpdatedAt       DATETIME2       NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT FK_WorkoutPlans_Trainer FOREIGN KEY (TrainerId) REFERENCES Users(UserId),
    CONSTRAINT FK_WorkoutPlans_Client  FOREIGN KEY (ClientId)  REFERENCES Users(UserId)
);
GO

-- ─────────────────────────────────────────────────────
-- 12. WORKOUT_DAYS — дни тренировки в плане
-- ─────────────────────────────────────────────────────
CREATE TABLE WorkoutDays (
    DayId           INT             PRIMARY KEY IDENTITY(1,1),
    PlanId          INT             NOT NULL,
    DayNumber       INT             NOT NULL,   -- 1, 2, 3...
    DayName         NVARCHAR(100)   NULL,       -- 'День 1 — Грудь/Трицепс'
    Notes           NVARCHAR(500)   NULL,

    CONSTRAINT FK_WorkoutDays_Plan FOREIGN KEY (PlanId) REFERENCES WorkoutPlans(PlanId) ON DELETE CASCADE
);
GO

-- ─────────────────────────────────────────────────────
-- 13. EXERCISES — упражнения в дне тренировки
-- ─────────────────────────────────────────────────────
CREATE TABLE Exercises (
    ExerciseId      INT             PRIMARY KEY IDENTITY(1,1),
    DayId           INT             NOT NULL,
    OrderIndex      INT             NOT NULL DEFAULT 1,
    Name            NVARCHAR(255)   NOT NULL,
    MuscleGroup     NVARCHAR(100)   NULL,
    Sets            INT             NULL,
    Reps            NVARCHAR(50)    NULL,   -- '8-12' или '15'
    WeightKg        DECIMAL(6,2)    NULL,
    RestSeconds     INT             NULL,
    VideoUrl        NVARCHAR(500)   NULL,
    Notes           NVARCHAR(500)   NULL,

    CONSTRAINT FK_Exercises_Day FOREIGN KEY (DayId) REFERENCES WorkoutDays(DayId) ON DELETE CASCADE
);
GO

-- ─────────────────────────────────────────────────────
-- 14. WORKOUT_LOGS — журнал выполненных тренировок
-- ─────────────────────────────────────────────────────
CREATE TABLE WorkoutLogs (
    LogId           INT             PRIMARY KEY IDENTITY(1,1),
    ClientId        INT             NOT NULL,
    PlanId          INT             NULL,
    DayId           INT             NULL,
    WorkoutDate     DATE            NOT NULL DEFAULT CAST(GETUTCDATE() AS DATE),
    DurationMinutes INT             NULL,
    Notes           NVARCHAR(500)   NULL,
    IsCompleted     BIT             NOT NULL DEFAULT 0,
    CreatedAt       DATETIME2       NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT FK_WorkoutLogs_Client FOREIGN KEY (ClientId) REFERENCES Users(UserId),
    CONSTRAINT FK_WorkoutLogs_Plan   FOREIGN KEY (PlanId)   REFERENCES WorkoutPlans(PlanId),
    CONSTRAINT FK_WorkoutLogs_Day    FOREIGN KEY (DayId)    REFERENCES WorkoutDays(DayId)
);
GO

-- ─────────────────────────────────────────────────────
-- 15. EXERCISE_LOGS — результаты по каждому упражнению
-- ─────────────────────────────────────────────────────
CREATE TABLE ExerciseLogs (
    ExerciseLogId   INT             PRIMARY KEY IDENTITY(1,1),
    WorkoutLogId    INT             NOT NULL,
    ExerciseId      INT             NULL,
    ExerciseName    NVARCHAR(255)   NOT NULL,   -- копия на случай удаления упражнения
    SetNumber       INT             NOT NULL,
    RepsActual      INT             NULL,
    WeightActualKg  DECIMAL(6,2)    NULL,
    Notes           NVARCHAR(255)   NULL,

    CONSTRAINT FK_ExerciseLogs_WorkoutLog FOREIGN KEY (WorkoutLogId) REFERENCES WorkoutLogs(LogId) ON DELETE CASCADE,
    CONSTRAINT FK_ExerciseLogs_Exercise   FOREIGN KEY (ExerciseId)   REFERENCES Exercises(ExerciseId)
);
GO

-- ─────────────────────────────────────────────────────
-- 16. MESSAGES — чат тренера и клиента
-- ─────────────────────────────────────────────────────
CREATE TABLE Messages (
    MessageId       INT             PRIMARY KEY IDENTITY(1,1),
    SenderId        INT             NOT NULL,
    ReceiverId      INT             NOT NULL,
    MessageText     NVARCHAR(MAX)   NULL,
    AttachmentUrl   NVARCHAR(500)   NULL,
    AttachmentType  NVARCHAR(20)    NULL    -- 'image', 'file'
        CHECK (AttachmentType IN ('image','file', NULL)),
    IsRead          BIT             NOT NULL DEFAULT 0,
    SentAt          DATETIME2       NOT NULL DEFAULT GETUTCDATE(),
    ReadAt          DATETIME2       NULL,
    IsDeleted       BIT             NOT NULL DEFAULT 0,

    CONSTRAINT FK_Messages_Sender   FOREIGN KEY (SenderId)   REFERENCES Users(UserId),
    CONSTRAINT FK_Messages_Receiver FOREIGN KEY (ReceiverId) REFERENCES Users(UserId),
    CONSTRAINT CK_Messages_NotSelf  CHECK (SenderId <> ReceiverId)
);
GO

-- ─────────────────────────────────────────────────────
-- 17. NOTIFICATIONS — уведомления
-- ─────────────────────────────────────────────────────
CREATE TABLE Notifications (
    NotificationId  INT             PRIMARY KEY IDENTITY(1,1),
    UserId          INT             NOT NULL,
    Title           NVARCHAR(255)   NOT NULL,
    Body            NVARCHAR(500)   NULL,
    NotifType       NVARCHAR(50)    NULL,   -- 'message', 'plan_assigned', 'reminder', 'measurement'
    EntityId        INT             NULL,   -- ID связанной сущности
    IsRead          BIT             NOT NULL DEFAULT 0,
    CreatedAt       DATETIME2       NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT FK_Notifications_User FOREIGN KEY (UserId) REFERENCES Users(UserId) ON DELETE CASCADE
);
GO

-- ─────────────────────────────────────────────────────
-- 18. FCM_TOKENS — токены для push-уведомлений
-- ─────────────────────────────────────────────────────
CREATE TABLE FcmTokens (
    TokenId         INT             PRIMARY KEY IDENTITY(1,1),
    UserId          INT             NOT NULL,
    DeviceToken     NVARCHAR(512)   NOT NULL,
    DevicePlatform  NVARCHAR(20)    NOT NULL DEFAULT 'android',
    CreatedAt       DATETIME2       NOT NULL DEFAULT GETUTCDATE(),
    UpdatedAt       DATETIME2       NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT FK_FcmTokens_User FOREIGN KEY (UserId) REFERENCES Users(UserId) ON DELETE CASCADE
);
GO
