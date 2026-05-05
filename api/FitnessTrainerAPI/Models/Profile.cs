namespace FitnessTrainerAPI.Models;

public class TrainerProfile
{
    public int TrainerProfileId { get; set; }
    public int UserId { get; set; }
    public string? Specialization { get; set; }
    public int? Experience { get; set; }
    public string? Description { get; set; }
    public string? CertificateUrl { get; set; }

    public User User { get; set; } = null!;
}

public class ClientProfile
{
    public int ClientProfileId { get; set; }
    public int UserId { get; set; }
    public int? TrainerId { get; set; }
    public DateOnly? BirthDate { get; set; }
    public string? Gender { get; set; }
    public decimal? HeightCm { get; set; }
    public decimal? InitialWeightKg { get; set; }
    public decimal? GoalWeight { get; set; }
    public string? ActivityLevel { get; set; }
    public string? FitnessGoal { get; set; }
    public int? DailyCalorieGoal { get; set; }
    public decimal? DailyProteinGoal { get; set; }
    public decimal? DailyFatGoal { get; set; }
    public decimal? DailyCarbGoal { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public User User { get; set; } = null!;
    public User? Trainer { get; set; }
}
