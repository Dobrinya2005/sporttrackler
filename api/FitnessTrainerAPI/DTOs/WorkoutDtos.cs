namespace FitnessTrainerAPI.DTOs;

public record WorkoutPlanCreateRequest(
    int ClientId,
    string Title,
    string? Description,
    DateOnly? StartDate,
    DateOnly? EndDate,
    List<WorkoutDayRequest> Days
);

public record WorkoutDayRequest(
    int DayNumber,
    string? DayName,
    string? Notes,
    List<ExerciseRequest> Exercises
);

public record ExerciseRequest(
    int OrderIndex,
    string Name,
    string? MuscleGroup,
    int? Sets,
    string? Reps,
    decimal? WeightKg,
    int? RestSeconds,
    string? VideoUrl,
    string? Notes
);

public record WorkoutPlanResponse(
    int PlanId,
    int TrainerId,
    int ClientId,
    string Title,
    string? Description,
    DateOnly? StartDate,
    DateOnly? EndDate,
    bool IsActive,
    List<WorkoutDayResponse> Days
);

public record WorkoutDayResponse(
    int DayId,
    int DayNumber,
    string? DayName,
    List<ExerciseResponse> Exercises
);

public record ExerciseResponse(
    int ExerciseId,
    int OrderIndex,
    string Name,
    string? MuscleGroup,
    int? Sets,
    string? Reps,
    decimal? WeightKg,
    int? RestSeconds,
    string? VideoUrl,
    string? Notes
);

public record WorkoutLogCreateRequest(
    int? PlanId,
    int? DayId,
    DateOnly? WorkoutDate,
    int? DurationMinutes,
    string? Notes,
    bool IsCompleted,
    List<ExerciseLogRequest> ExerciseLogs
);

public record ExerciseLogRequest(
    int? ExerciseId,
    string ExerciseName,
    int SetNumber,
    int? RepsActual,
    decimal? WeightActualKg,
    string? Notes
);
