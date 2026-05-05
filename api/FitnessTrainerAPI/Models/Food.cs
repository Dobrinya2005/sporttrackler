namespace FitnessTrainerAPI.Models;

public class FoodProduct
{
    public int ProductId { get; set; }
    public string? ExternalId { get; set; }
    public string Name { get; set; } = string.Empty;
    public string? Brand { get; set; }
    public decimal CaloriesPer100g { get; set; }
    public decimal ProteinPer100g { get; set; }
    public decimal FatPer100g { get; set; }
    public decimal CarbsPer100g { get; set; }
    public decimal? FiberPer100g { get; set; }
    public bool IsCustom { get; set; }
    public int? CreatedBy { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public User? Creator { get; set; }
    public ICollection<FoodDiaryEntry> DiaryEntries { get; set; } = [];
}

public class FoodDiaryEntry
{
    public int DiaryId { get; set; }
    public int ClientId { get; set; }
    public DateOnly DiaryDate { get; set; } = DateOnly.FromDateTime(DateTime.UtcNow);
    public string MealType { get; set; } = string.Empty;
    public int ProductId { get; set; }
    public decimal WeightG { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public User Client { get; set; } = null!;
    public FoodProduct Product { get; set; } = null!;
}
