-- =====================================================
-- FITNESS TRAINER APP — VIEWS
-- =====================================================

USE FitnessTrainerDB;
GO

-- ─────────────────────────────────────────────────────
-- V_ClientList — список клиентов тренера
-- ─────────────────────────────────────────────────────
CREATE OR ALTER VIEW V_ClientList AS
SELECT
    u.UserId,
    u.FirstName,
    u.LastName,
    u.Email,
    u.Phone,
    u.AvatarUrl,
    cp.TrainerId,
    cp.BirthDate,
    cp.Gender,
    cp.HeightCm,
    cp.FitnessGoal,
    cp.ActivityLevel,
    cp.DailyCalorieGoal,
    cp.DailyProteinGoal,
    cp.DailyFatGoal,
    cp.DailyCarbGoal
FROM Users u
INNER JOIN ClientProfiles cp ON u.UserId = cp.UserId
WHERE u.IsActive = 1;
GO

-- ─────────────────────────────────────────────────────
-- V_LatestMeasurements — последний замер каждого клиента
-- ─────────────────────────────────────────────────────
CREATE OR ALTER VIEW V_LatestMeasurements AS
SELECT m.*
FROM Measurements m
INNER JOIN (
    SELECT ClientId, MAX(MeasuredAt) AS LastDate
    FROM Measurements
    GROUP BY ClientId
) latest ON m.ClientId = latest.ClientId AND m.MeasuredAt = latest.LastDate;
GO

-- ─────────────────────────────────────────────────────
-- V_FoodDiaryDetailed — дневник питания с деталями продуктов
-- ─────────────────────────────────────────────────────
CREATE OR ALTER VIEW V_FoodDiaryDetailed AS
SELECT
    fd.DiaryId,
    fd.ClientId,
    fd.DiaryDate,
    fd.MealType,
    fd.WeightG,
    fp.ProductId,
    fp.Name                                                     AS ProductName,
    fp.Brand,
    ROUND(fd.WeightG / 100.0 * fp.CaloriesPer100g, 2)          AS Calories,
    ROUND(fd.WeightG / 100.0 * fp.ProteinPer100g,  2)          AS Protein,
    ROUND(fd.WeightG / 100.0 * fp.FatPer100g,      2)          AS Fat,
    ROUND(fd.WeightG / 100.0 * fp.CarbsPer100g,    2)          AS Carbs,
    fp.CaloriesPer100g,
    fp.ProteinPer100g,
    fp.FatPer100g,
    fp.CarbsPer100g
FROM FoodDiary fd
INNER JOIN FoodProducts fp ON fd.ProductId = fp.ProductId;
GO

-- ─────────────────────────────────────────────────────
-- V_UnreadMessages — количество непрочитанных по диалогам
-- ─────────────────────────────────────────────────────
CREATE OR ALTER VIEW V_UnreadMessages AS
SELECT
    m.ReceiverId,
    m.SenderId,
    COUNT(*) AS UnreadCount,
    MAX(m.SentAt) AS LastMessageAt
FROM Messages m
WHERE m.IsRead = 0 AND m.IsDeleted = 0
GROUP BY m.ReceiverId, m.SenderId;
GO

-- ─────────────────────────────────────────────────────
-- V_WorkoutPlanFull — полный план тренировок
-- ─────────────────────────────────────────────────────
CREATE OR ALTER VIEW V_WorkoutPlanFull AS
SELECT
    wp.PlanId,
    wp.Title                AS PlanTitle,
    wp.Description          AS PlanDescription,
    wp.StartDate,
    wp.EndDate,
    wp.IsActive,
    wp.TrainerId,
    wp.ClientId,
    wd.DayId,
    wd.DayNumber,
    wd.DayName,
    e.ExerciseId,
    e.OrderIndex,
    e.Name                  AS ExerciseName,
    e.MuscleGroup,
    e.Sets,
    e.Reps,
    e.WeightKg,
    e.RestSeconds,
    e.Notes                 AS ExerciseNotes
FROM WorkoutPlans wp
INNER JOIN WorkoutDays wd  ON wp.PlanId  = wd.PlanId
INNER JOIN Exercises   e   ON wd.DayId   = e.DayId
WHERE wp.IsActive = 1;
GO
