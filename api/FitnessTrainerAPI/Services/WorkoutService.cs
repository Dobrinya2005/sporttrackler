using FitnessTrainerAPI.Data;
using FitnessTrainerAPI.DTOs;
using FitnessTrainerAPI.Models;
using Microsoft.EntityFrameworkCore;

namespace FitnessTrainerAPI.Services;

public interface IWorkoutService
{
    Task<List<WorkoutPlanResponse>> GetPlansAsync(int clientId);
    Task<WorkoutPlanResponse> CreatePlanAsync(int trainerId, WorkoutPlanCreateRequest request);
    Task<bool> DeletePlanAsync(int planId, int trainerId);
}

public class WorkoutService(AppDbContext db) : IWorkoutService
{
    public async Task<List<WorkoutPlanResponse>> GetPlansAsync(int clientId) =>
        await db.WorkoutPlans
            .Where(p => p.ClientId == clientId)
            .Include(p => p.Days).ThenInclude(d => d.Exercises)
            .OrderByDescending(p => p.IsActive).ThenByDescending(p => p.CreatedAt)
            .Select(p => ToResponse(p))
            .ToListAsync();

    public async Task<WorkoutPlanResponse> CreatePlanAsync(int trainerId, WorkoutPlanCreateRequest req)
    {
        var plan = new WorkoutPlan
        {
            TrainerId   = trainerId,
            ClientId    = req.ClientId,
            Title       = req.Title,
            Description = req.Description,
            StartDate   = req.StartDate,
            EndDate     = req.EndDate,
            IsActive    = true,
            Days = req.Days.Select(d => new WorkoutDay
            {
                DayNumber = d.DayNumber,
                DayName   = d.DayName,
                Notes     = d.Notes,
                Exercises = d.Exercises.Select(e => new Exercise
                {
                    OrderIndex  = e.OrderIndex,
                    Name        = e.Name,
                    MuscleGroup = e.MuscleGroup,
                    Sets        = e.Sets,
                    Reps        = e.Reps,
                    WeightKg    = e.WeightKg,
                    RestSeconds = e.RestSeconds,
                    VideoUrl    = e.VideoUrl,
                    Notes       = e.Notes
                }).ToList()
            }).ToList()
        };

        db.WorkoutPlans.Add(plan);
        await db.SaveChangesAsync();

        await db.Entry(plan).Collection(p => p.Days).Query()
            .Include(d => d.Exercises).LoadAsync();

        return ToResponse(plan);
    }

    public async Task<bool> DeletePlanAsync(int planId, int trainerId)
    {
        var plan = await db.WorkoutPlans.FindAsync(planId);
        if (plan is null || plan.TrainerId != trainerId) return false;
        db.WorkoutPlans.Remove(plan);
        await db.SaveChangesAsync();
        return true;
    }

    private static WorkoutPlanResponse ToResponse(WorkoutPlan p) => new(
        p.PlanId, p.TrainerId, p.ClientId,
        p.Title, p.Description, p.StartDate, p.EndDate, p.IsActive,
        p.Days.OrderBy(d => d.DayNumber).Select(d => new WorkoutDayResponse(
            d.DayId, d.DayNumber, d.DayName,
            d.Exercises.OrderBy(e => e.OrderIndex).Select(e => new ExerciseResponse(
                e.ExerciseId, e.OrderIndex, e.Name, e.MuscleGroup,
                e.Sets, e.Reps, e.WeightKg, e.RestSeconds, e.VideoUrl, e.Notes
            )).ToList()
        )).ToList()
    );
}
