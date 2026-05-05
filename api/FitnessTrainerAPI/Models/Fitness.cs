namespace FitnessTrainerAPI.Models;

public class Measurement
{
    public int MeasurementId { get; set; }
    public int ClientId { get; set; }
    public DateOnly MeasuredAt { get; set; } = DateOnly.FromDateTime(DateTime.UtcNow);
    public decimal? WeightKg { get; set; }
    public decimal? HeightCm { get; set; }
    public decimal? ChestCm { get; set; }
    public decimal? WaistCm { get; set; }
    public decimal? HipsCm { get; set; }
    public decimal? NeckCm { get; set; }
    public decimal? BicepCm { get; set; }
    public decimal? ForearmCm { get; set; }
    public decimal? ThighCm { get; set; }
    public decimal? CalfCm { get; set; }
    public decimal? BodyFatPercent { get; set; }
    public decimal? MuscleMassKg { get; set; }
    public decimal? BMI { get; set; }
    public string? Notes { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public User Client { get; set; } = null!;
}

public class ProgressPhoto
{
    public int PhotoId { get; set; }
    public int ClientId { get; set; }
    public string PhotoUrl { get; set; } = string.Empty;
    public string? ThumbnailUrl { get; set; }
    public DateOnly PhotoDate { get; set; } = DateOnly.FromDateTime(DateTime.UtcNow);
    public string? PoseType { get; set; }
    public string? Description { get; set; }
    public bool IsVisibleToTrainer { get; set; } = true;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public User Client { get; set; } = null!;
}

public class WorkoutPlan
{
    public int PlanId { get; set; }
    public int TrainerId { get; set; }
    public int ClientId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string? Description { get; set; }
    public DateOnly? StartDate { get; set; }
    public DateOnly? EndDate { get; set; }
    public bool IsActive { get; set; } = true;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    public User Trainer { get; set; } = null!;
    public User Client { get; set; } = null!;
    public ICollection<WorkoutDay> Days { get; set; } = [];
}

public class WorkoutDay
{
    public int DayId { get; set; }
    public int PlanId { get; set; }
    public int DayNumber { get; set; }
    public string? DayName { get; set; }
    public string? Notes { get; set; }

    public WorkoutPlan Plan { get; set; } = null!;
    public ICollection<Exercise> Exercises { get; set; } = [];
}

public class Exercise
{
    public int ExerciseId { get; set; }
    public int DayId { get; set; }
    public int OrderIndex { get; set; } = 1;
    public string Name { get; set; } = string.Empty;
    public string? MuscleGroup { get; set; }
    public int? Sets { get; set; }
    public string? Reps { get; set; }
    public decimal? WeightKg { get; set; }
    public int? RestSeconds { get; set; }
    public string? VideoUrl { get; set; }
    public string? Notes { get; set; }

    public WorkoutDay Day { get; set; } = null!;
}

public class WorkoutLog
{
    public int LogId { get; set; }
    public int ClientId { get; set; }
    public int? PlanId { get; set; }
    public int? DayId { get; set; }
    public DateOnly WorkoutDate { get; set; } = DateOnly.FromDateTime(DateTime.UtcNow);
    public int? DurationMinutes { get; set; }
    public string? Notes { get; set; }
    public bool IsCompleted { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public User Client { get; set; } = null!;
    public ICollection<ExerciseLog> ExerciseLogs { get; set; } = [];
}

public class ExerciseLog
{
    public int ExerciseLogId { get; set; }
    public int WorkoutLogId { get; set; }
    public int? ExerciseId { get; set; }
    public string ExerciseName { get; set; } = string.Empty;
    public int SetNumber { get; set; }
    public int? RepsActual { get; set; }
    public decimal? WeightActualKg { get; set; }
    public string? Notes { get; set; }

    public WorkoutLog WorkoutLog { get; set; } = null!;
}
