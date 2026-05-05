namespace FitnessTrainerAPI.DTOs;

public record MeasurementCreateRequest(
    DateOnly? MeasuredAt,
    decimal? WeightKg,
    decimal? HeightCm,
    decimal? ChestCm,
    decimal? WaistCm,
    decimal? HipsCm,
    decimal? NeckCm,
    decimal? BicepCm,
    decimal? ForearmCm,
    decimal? ThighCm,
    decimal? CalfCm,
    decimal? BodyFatPercent,
    decimal? MuscleMassKg,
    string? Notes
);

public record MeasurementResponse(
    int MeasurementId,
    int ClientId,
    DateOnly MeasuredAt,
    decimal? WeightKg,
    decimal? HeightCm,
    decimal? ChestCm,
    decimal? WaistCm,
    decimal? HipsCm,
    decimal? NeckCm,
    decimal? BicepCm,
    decimal? ForearmCm,
    decimal? ThighCm,
    decimal? CalfCm,
    decimal? BodyFatPercent,
    decimal? MuscleMassKg,
    decimal? BMI,
    string? Notes,
    DateTime CreatedAt
);
