-- =====================================================
-- FITNESS TRAINER APP — STORED PROCEDURES
-- =====================================================

USE FitnessTrainerDB;
GO

-- ─────────────────────────────────────────────────────
-- SP_GetClientDailySummary
-- Суммарное КБЖУ клиента за день
-- ─────────────────────────────────────────────────────
CREATE OR ALTER PROCEDURE SP_GetClientDailySummary
    @ClientId   INT,
    @DiaryDate  DATE
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        fd.DiaryDate,
        fd.MealType,
        SUM(ROUND(fd.WeightG / 100.0 * fp.CaloriesPer100g, 2))  AS TotalCalories,
        SUM(ROUND(fd.WeightG / 100.0 * fp.ProteinPer100g,  2))  AS TotalProtein,
        SUM(ROUND(fd.WeightG / 100.0 * fp.FatPer100g,      2))  AS TotalFat,
        SUM(ROUND(fd.WeightG / 100.0 * fp.CarbsPer100g,    2))  AS TotalCarbs
    FROM FoodDiary fd
    INNER JOIN FoodProducts fp ON fd.ProductId = fp.ProductId
    WHERE fd.ClientId = @ClientId
      AND fd.DiaryDate = @DiaryDate
    GROUP BY fd.DiaryDate, fd.MealType

    UNION ALL

    SELECT
        @DiaryDate,
        'total',
        SUM(ROUND(fd.WeightG / 100.0 * fp.CaloriesPer100g, 2)),
        SUM(ROUND(fd.WeightG / 100.0 * fp.ProteinPer100g,  2)),
        SUM(ROUND(fd.WeightG / 100.0 * fp.FatPer100g,      2)),
        SUM(ROUND(fd.WeightG / 100.0 * fp.CarbsPer100g,    2))
    FROM FoodDiary fd
    INNER JOIN FoodProducts fp ON fd.ProductId = fp.ProductId
    WHERE fd.ClientId = @ClientId
      AND fd.DiaryDate = @DiaryDate;
END
GO

-- ─────────────────────────────────────────────────────
-- SP_GetClientLatestMeasurement
-- Последний замер клиента
-- ─────────────────────────────────────────────────────
CREATE OR ALTER PROCEDURE SP_GetClientLatestMeasurement
    @ClientId INT
AS
BEGIN
    SET NOCOUNT ON;

    SELECT TOP 1 *
    FROM Measurements
    WHERE ClientId = @ClientId
    ORDER BY MeasuredAt DESC, CreatedAt DESC;
END
GO

-- ─────────────────────────────────────────────────────
-- SP_GetClientMeasurementHistory
-- История замеров за период
-- ─────────────────────────────────────────────────────
CREATE OR ALTER PROCEDURE SP_GetClientMeasurementHistory
    @ClientId   INT,
    @DateFrom   DATE = NULL,
    @DateTo     DATE = NULL
AS
BEGIN
    SET NOCOUNT ON;

    SELECT *
    FROM Measurements
    WHERE ClientId = @ClientId
      AND (@DateFrom IS NULL OR MeasuredAt >= @DateFrom)
      AND (@DateTo   IS NULL OR MeasuredAt <= @DateTo)
    ORDER BY MeasuredAt ASC;
END
GO

-- ─────────────────────────────────────────────────────
-- SP_GetConversation
-- Переписка между двумя пользователями
-- ─────────────────────────────────────────────────────
CREATE OR ALTER PROCEDURE SP_GetConversation
    @UserId1    INT,
    @UserId2    INT,
    @PageSize   INT = 50,
    @PageNumber INT = 1
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        m.MessageId,
        m.SenderId,
        m.ReceiverId,
        m.MessageText,
        m.AttachmentUrl,
        m.AttachmentType,
        m.IsRead,
        m.SentAt,
        u.FirstName + ' ' + u.LastName AS SenderName,
        u.AvatarUrl AS SenderAvatar
    FROM Messages m
    INNER JOIN Users u ON m.SenderId = u.UserId
    WHERE m.IsDeleted = 0
      AND (
          (m.SenderId = @UserId1 AND m.ReceiverId = @UserId2) OR
          (m.SenderId = @UserId2 AND m.ReceiverId = @UserId1)
      )
    ORDER BY m.SentAt DESC
    OFFSET (@PageNumber - 1) * @PageSize ROWS
    FETCH NEXT @PageSize ROWS ONLY;
END
GO

-- ─────────────────────────────────────────────────────
-- SP_MarkMessagesAsRead
-- Пометить сообщения как прочитанные
-- ─────────────────────────────────────────────────────
CREATE OR ALTER PROCEDURE SP_MarkMessagesAsRead
    @ReceiverId INT,
    @SenderId   INT
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE Messages
    SET IsRead = 1,
        ReadAt = GETUTCDATE()
    WHERE ReceiverId = @ReceiverId
      AND SenderId   = @SenderId
      AND IsRead     = 0;
END
GO

-- ─────────────────────────────────────────────────────
-- SP_GetTrainerClients
-- Список клиентов тренера с последними данными
-- ─────────────────────────────────────────────────────
CREATE OR ALTER PROCEDURE SP_GetTrainerClients
    @TrainerId INT
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        u.UserId,
        u.FirstName,
        u.LastName,
        u.Email,
        u.AvatarUrl,
        cp.FitnessGoal,
        cp.DailyCalorieGoal,
        m.WeightKg          AS LastWeight,
        m.BodyFatPercent    AS LastBodyFat,
        m.MeasuredAt        AS LastMeasuredAt,
        (
            SELECT COUNT(*) FROM Messages
            WHERE ReceiverId = @TrainerId
              AND SenderId   = u.UserId
              AND IsRead     = 0
        ) AS UnreadMessages
    FROM Users u
    INNER JOIN ClientProfiles cp ON u.UserId = cp.UserId
    OUTER APPLY (
        SELECT TOP 1 WeightKg, BodyFatPercent, MeasuredAt
        FROM Measurements
        WHERE ClientId = u.UserId
        ORDER BY MeasuredAt DESC
    ) m
    WHERE cp.TrainerId = @TrainerId
      AND u.IsActive   = 1;
END
GO

-- ─────────────────────────────────────────────────────
-- SP_CalculateCalorieGoal
-- Расчёт нормы калорий по формуле Миффлина-Сан Жеора
-- ─────────────────────────────────────────────────────
CREATE OR ALTER PROCEDURE SP_CalculateCalorieGoal
    @UserId INT
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @Weight     DECIMAL(5,2),
            @Height     DECIMAL(5,1),
            @Age        INT,
            @Gender     NCHAR(1),
            @Activity   NVARCHAR(50),
            @Goal       NVARCHAR(100),
            @BMR        DECIMAL(8,2),
            @TDEE       DECIMAL(8,2),
            @ActivityK  DECIMAL(4,2);

    SELECT
        @Weight   = cp.InitialWeightKg,
        @Height   = cp.HeightCm,
        @Age      = DATEDIFF(YEAR, cp.BirthDate, GETDATE()),
        @Gender   = cp.Gender,
        @Activity = cp.ActivityLevel,
        @Goal     = cp.FitnessGoal
    FROM ClientProfiles cp
    WHERE cp.UserId = @UserId;

    -- Формула Миффлина-Сан Жеора
    IF @Gender = 'M'
        SET @BMR = 10 * @Weight + 6.25 * @Height - 5 * @Age + 5;
    ELSE
        SET @BMR = 10 * @Weight + 6.25 * @Height - 5 * @Age - 161;

    -- Коэффициент активности
    SET @ActivityK = CASE @Activity
        WHEN 'sedentary'    THEN 1.2
        WHEN 'light'        THEN 1.375
        WHEN 'moderate'     THEN 1.55
        WHEN 'active'       THEN 1.725
        WHEN 'very_active'  THEN 1.9
        ELSE 1.2
    END;

    SET @TDEE = @BMR * @ActivityK;

    -- Корректировка под цель
    DECLARE @CalorieGoal DECIMAL(8,2);
    SET @CalorieGoal = CASE
        WHEN @Goal LIKE '%похудени%'  OR @Goal LIKE '%loss%'    THEN @TDEE - 500
        WHEN @Goal LIKE '%масс%'      OR @Goal LIKE '%gain%'    THEN @TDEE + 300
        ELSE @TDEE
    END;

    -- Обновляем профиль клиента
    UPDATE ClientProfiles
    SET DailyCalorieGoal = ROUND(@CalorieGoal, 0),
        DailyProteinGoal = ROUND(@Weight * 2.0, 1),   -- 2г/кг веса
        DailyFatGoal     = ROUND(@CalorieGoal * 0.25 / 9, 1),
        DailyCarbGoal    = ROUND((@CalorieGoal - @Weight * 2.0 * 4 - @CalorieGoal * 0.25) / 4, 1)
    WHERE UserId = @UserId;

    SELECT
        ROUND(@BMR, 0)          AS BMR,
        ROUND(@TDEE, 0)         AS TDEE,
        ROUND(@CalorieGoal, 0)  AS CalorieGoal,
        ROUND(@Weight * 2.0, 1) AS ProteinGoal;
END
GO

-- ─────────────────────────────────────────────────────
-- SP_GetClientProgressSummary
-- Сводка прогресса клиента для тренера
-- ─────────────────────────────────────────────────────
CREATE OR ALTER PROCEDURE SP_GetClientProgressSummary
    @ClientId INT
AS
BEGIN
    SET NOCOUNT ON;

    -- Первый и последний замер
    SELECT
        first_m.MeasuredAt      AS FirstDate,
        first_m.WeightKg        AS StartWeight,
        last_m.MeasuredAt       AS LastDate,
        last_m.WeightKg         AS CurrentWeight,
        last_m.WeightKg - first_m.WeightKg AS WeightChange,
        last_m.BodyFatPercent   AS CurrentBodyFat,
        last_m.BMI              AS CurrentBMI
    FROM
        (SELECT TOP 1 MeasuredAt, WeightKg, BodyFatPercent, BMI FROM Measurements WHERE ClientId = @ClientId ORDER BY MeasuredAt ASC)  first_m,
        (SELECT TOP 1 MeasuredAt, WeightKg, BodyFatPercent, BMI FROM Measurements WHERE ClientId = @ClientId ORDER BY MeasuredAt DESC) last_m;

    -- Кол-во тренировок за последние 30 дней
    SELECT COUNT(*) AS WorkoutsLast30Days
    FROM WorkoutLogs
    WHERE ClientId = @ClientId
      AND IsCompleted = 1
      AND WorkoutDate >= DATEADD(DAY, -30, GETDATE());

    -- Среднее КБЖУ за последние 7 дней
    SELECT
        ROUND(AVG(daily.TotalCalories), 0) AS AvgCalories,
        ROUND(AVG(daily.TotalProtein),  1) AS AvgProtein,
        ROUND(AVG(daily.TotalFat),      1) AS AvgFat,
        ROUND(AVG(daily.TotalCarbs),    1) AS AvgCarbs
    FROM (
        SELECT
            fd.DiaryDate,
            SUM(fd.WeightG / 100.0 * fp.CaloriesPer100g) AS TotalCalories,
            SUM(fd.WeightG / 100.0 * fp.ProteinPer100g)  AS TotalProtein,
            SUM(fd.WeightG / 100.0 * fp.FatPer100g)      AS TotalFat,
            SUM(fd.WeightG / 100.0 * fp.CarbsPer100g)    AS TotalCarbs
        FROM FoodDiary fd
        INNER JOIN FoodProducts fp ON fd.ProductId = fp.ProductId
        WHERE fd.ClientId  = @ClientId
          AND fd.DiaryDate >= DATEADD(DAY, -7, GETDATE())
        GROUP BY fd.DiaryDate
    ) daily;
END
GO
