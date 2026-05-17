namespace FitnessTrainerAPI.DTOs;

public class SaveClientProfileRequest
{
    public string? FitnessGoal   { get; set; }
    public string? ActivityLevel { get; set; }
    public decimal? WeightKg     { get; set; }
    public decimal? HeightCm     { get; set; }
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
