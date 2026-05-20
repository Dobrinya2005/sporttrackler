using System.ComponentModel.DataAnnotations;

namespace FitnessTrainerAPI.DTOs;

public class SaveClientProfileRequest
{
    public string? FitnessGoal   { get; set; }
    public string? ActivityLevel { get; set; }
    [Range(20, 300, ErrorMessage = "Вес должен быть от 20 до 300 кг")]
    public decimal? WeightKg     { get; set; }
    [Range(50, 250, ErrorMessage = "Рост должен быть от 50 до 250 см")]
    public decimal? HeightCm     { get; set; }
    [Range(20, 300, ErrorMessage = "Целевой вес должен быть от 20 до 300 кг")]
    public decimal? GoalWeightKg { get; set; }
    public DateOnly? BirthDate   { get; set; }
    public string? Gender        { get; set; }
}

public record ClientProfileResponse(
    int UserId,
    string? FitnessGoal,
    string? ActivityLevel,
    decimal? WeightKg,
    decimal? HeightCm,
    decimal? GoalWeightKg,
    DateOnly? BirthDate,
    string? Gender
);
