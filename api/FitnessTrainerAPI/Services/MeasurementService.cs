using FitnessTrainerAPI.Data;
using FitnessTrainerAPI.DTOs;
using FitnessTrainerAPI.Models;
using Microsoft.EntityFrameworkCore;

namespace FitnessTrainerAPI.Services;

public interface IMeasurementService
{
    Task<MeasurementResponse> AddAsync(int clientId, MeasurementCreateRequest request);
    Task<List<MeasurementResponse>> GetHistoryAsync(int clientId, DateOnly? from, DateOnly? to);
    Task<MeasurementResponse?> GetLatestAsync(int clientId);
    Task<bool> DeleteAsync(int measurementId, int requestingUserId);
}

public class MeasurementService(AppDbContext db) : IMeasurementService
{
    public async Task<MeasurementResponse> AddAsync(int clientId, MeasurementCreateRequest req)
    {
        var m = new Measurement
        {
            ClientId       = clientId,
            MeasuredAt     = req.MeasuredAt ?? DateOnly.FromDateTime(DateTime.UtcNow),
            WeightKg       = req.WeightKg,
            HeightCm       = req.HeightCm,
            ChestCm        = req.ChestCm,
            WaistCm        = req.WaistCm,
            HipsCm         = req.HipsCm,
            NeckCm         = req.NeckCm,
            BicepCm        = req.BicepCm,
            ForearmCm      = req.ForearmCm,
            ThighCm        = req.ThighCm,
            CalfCm         = req.CalfCm,
            BodyFatPercent = req.BodyFatPercent,
            MuscleMassKg   = req.MuscleMassKg,
            Notes          = req.Notes
        };

        db.Measurements.Add(m);
        await db.SaveChangesAsync();

        // перечитываем чтобы получить вычисляемый BMI
        await db.Entry(m).ReloadAsync();
        return ToResponse(m);
    }

    public async Task<List<MeasurementResponse>> GetHistoryAsync(int clientId, DateOnly? from, DateOnly? to)
    {
        var query = db.Measurements.Where(m => m.ClientId == clientId);
        if (from.HasValue) query = query.Where(m => m.MeasuredAt >= from.Value);
        if (to.HasValue)   query = query.Where(m => m.MeasuredAt <= to.Value);

        return await query
            .OrderBy(m => m.MeasuredAt)
            .Select(m => ToResponse(m))
            .ToListAsync();
    }

    public async Task<MeasurementResponse?> GetLatestAsync(int clientId)
    {
        var m = await db.Measurements
            .Where(m => m.ClientId == clientId)
            .OrderByDescending(m => m.MeasuredAt)
            .FirstOrDefaultAsync();

        return m is null ? null : ToResponse(m);
    }

    public async Task<bool> DeleteAsync(int measurementId, int requestingUserId)
    {
        var m = await db.Measurements.FindAsync(measurementId);
        if (m is null || m.ClientId != requestingUserId) return false;
        db.Measurements.Remove(m);
        await db.SaveChangesAsync();
        return true;
    }

    private static MeasurementResponse ToResponse(Measurement m) => new(
        m.MeasurementId, m.ClientId, m.MeasuredAt,
        m.WeightKg, m.HeightCm, m.ChestCm, m.WaistCm, m.HipsCm,
        m.NeckCm, m.BicepCm, m.ForearmCm, m.ThighCm, m.CalfCm,
        m.BodyFatPercent, m.MuscleMassKg, m.BMI, m.Notes, m.CreatedAt);
}
