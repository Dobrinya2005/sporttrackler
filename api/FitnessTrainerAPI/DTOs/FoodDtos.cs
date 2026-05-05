namespace FitnessTrainerAPI.DTOs;

public record FoodProductResponse(
    int ProductId,
    string? ExternalId,
    string Name,
    string? Brand,
    decimal CaloriesPer100g,
    decimal ProteinPer100g,
    decimal FatPer100g,
    decimal CarbsPer100g,
    decimal? FiberPer100g,
    bool IsCustom
);

public record FoodProductCreateRequest(
    string Name,
    string? Brand,
    decimal CaloriesPer100g,
    decimal ProteinPer100g,
    decimal FatPer100g,
    decimal CarbsPer100g,
    decimal? FiberPer100g
);

public record DiaryEntryCreateRequest(
    DateOnly? DiaryDate,
    string MealType,
    int ProductId,
    decimal WeightG
);

public record DiaryEntryResponse(
    int DiaryId,
    DateOnly DiaryDate,
    string MealType,
    FoodProductResponse Product,
    decimal WeightG,
    decimal Calories,
    decimal Protein,
    decimal Fat,
    decimal Carbs
);

public record DailySummaryResponse(
    DateOnly Date,
    int? CalorieGoal,
    decimal? ProteinGoal,
    decimal? FatGoal,
    decimal? CarbGoal,
    decimal TotalCalories,
    decimal TotalProtein,
    decimal TotalFat,
    decimal TotalCarbs,
    List<MealSummary> Meals
);

public record MealSummary(
    string MealType,
    decimal Calories,
    decimal Protein,
    decimal Fat,
    decimal Carbs,
    List<DiaryEntryResponse> Items
);
