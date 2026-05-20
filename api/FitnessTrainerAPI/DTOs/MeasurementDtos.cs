using System.ComponentModel.DataAnnotations;

namespace FitnessTrainerAPI.DTOs;

public record MeasurementCreateRequest(
    DateOnly? MeasuredAt,
    [Range(20, 300, ErrorMessage = "Вес должен быть от 20 до 300 кг")]
    decimal? WeightKg,
    [Range(50, 250, ErrorMessage = "Рост должен быть от 50 до 250 см")]
    decimal? HeightCm,
    [Range(40, 200, ErrorMessage = "Обхват груди должен быть от 40 до 200 см")]
    decimal? ChestCm,
    [Range(30, 200, ErrorMessage = "Обхват талии должен быть от 30 до 200 см")]
    decimal? WaistCm,
    [Range(40, 200, ErrorMessage = "Обхват бёдер должен быть от 40 до 200 см")]
    decimal? HipsCm,
    [Range(20, 80, ErrorMessage = "Обхват шеи должен быть от 20 до 80 см")]
    decimal? NeckCm,
    [Range(10, 80, ErrorMessage = "Обхват бицепса должен быть от 10 до 80 см")]
    decimal? BicepCm,
    [Range(10, 60, ErrorMessage = "Обхват предплечья должен быть от 10 до 60 см")]
    decimal? ForearmCm,
    [Range(20, 120, ErrorMessage = "Обхват бедра должен быть от 20 до 120 см")]
    decimal? ThighCm,
    [Range(15, 80, ErrorMessage = "Обхват икры должен быть от 15 до 80 см")]
    decimal? CalfCm,
    [Range(1, 60, ErrorMessage = "Процент жира должен быть от 1 до 60%")]
    decimal? BodyFatPercent,
    [Range(10, 200, ErrorMessage = "Мышечная масса должна быть от 10 до 200 кг")]
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
