using FitnessTrainerAPI.Data;
using FitnessTrainerAPI.DTOs;
using Microsoft.EntityFrameworkCore;

namespace FitnessTrainerAPI.Services;

public interface IClientProfileService
{
    Task<ClientProfileResponse?> SaveProfileAsync(int userId, SaveClientProfileRequest request);
    Task<ClientProfileResponse?> GetProfileAsync(int clientId);
    void RecalculateMacros(Models.ClientProfile profile);
}

public class ClientProfileService(AppDbContext db) : IClientProfileService
{
    public async Task<ClientProfileResponse?> SaveProfileAsync(int userId, SaveClientProfileRequest request)
    {
        var profile = await db.ClientProfiles.FirstOrDefaultAsync(p => p.UserId == userId);
        if (profile is null) return null;

        profile.FitnessGoal      = request.FitnessGoal;
        profile.ActivityLevel    = request.ActivityLevel;
        profile.InitialWeightKg  = request.WeightKg;
        profile.HeightCm         = request.HeightCm;
        profile.GoalWeight       = request.GoalWeightKg;
        profile.BirthDate        = request.BirthDate;
        profile.Gender           = request.Gender;

        CalculateMacroGoals(profile);

        await db.SaveChangesAsync();
        return MapToResponse(profile);
    }

    public async Task<ClientProfileResponse?> GetProfileAsync(int clientId)
    {
        var profile = await db.ClientProfiles
            .Include(p => p.User)
            .FirstOrDefaultAsync(p => p.UserId == clientId);

        if (profile is null) return null;

        if (profile.DailyCalorieGoal is null || profile.DailyProteinGoal is null)
        {
            CalculateMacroGoals(profile);
            await db.SaveChangesAsync();
        }

        return MapToResponse(profile);
    }

    public void RecalculateMacros(Models.ClientProfile p) => CalculateMacroGoals(p);

    private static void CalculateMacroGoals(Models.ClientProfile p)
    {
        if (p.InitialWeightKg is null || p.HeightCm is null) return;

        var weight = (double)p.InitialWeightKg;
        var height = (double)p.HeightCm;
        var age    = p.BirthDate.HasValue
            ? DateTime.Today.Year - p.BirthDate.Value.Year
            : 25; // default age if not provided

        // Mifflin-St Jeor BMR
        double bmr = p.Gender?.ToLower() == "female"
            ? 10 * weight + 6.25 * height - 5 * age - 161
            : 10 * weight + 6.25 * height - 5 * age + 5;

        double activityMultiplier = p.ActivityLevel switch
        {
            "beginner"     => 1.2,
            "intermediate" => 1.55,
            "advanced"     => 1.725,
            "sedentary"    => 1.2,
            "light"        => 1.375,
            "moderate"     => 1.55,
            "active"       => 1.725,
            "very_active"  => 1.9,
            _              => 1.375
        };

        double tdee = bmr * activityMultiplier;

        double calories = p.FitnessGoal switch
        {
            "lose_weight"  => tdee - 500,
            "weight_loss"  => tdee - 500,
            "gain_muscle"  => tdee + 300,
            "weight_gain"  => tdee + 300,
            _              => tdee  // maintain / endurance
        };

        calories = Math.Max(1200, calories);

        // Protein: 2g/kg; Fat: 25% of calories; Carbs: remainder
        double protein = weight * 2.0;
        double fat     = calories * 0.25 / 9.0;
        double carbs   = (calories - protein * 4 - fat * 9) / 4.0;

        p.DailyCalorieGoal = (int)Math.Round(calories);
        p.DailyProteinGoal = (decimal)Math.Round(protein, 1);
        p.DailyFatGoal     = (decimal)Math.Round(fat,     1);
        p.DailyCarbGoal    = (decimal)Math.Round(carbs,   1);
    }

    private static ClientProfileResponse MapToResponse(Models.ClientProfile p) => new(
        p.UserId,
        p.FitnessGoal,
        p.ActivityLevel,
        p.InitialWeightKg,
        p.HeightCm,
        p.GoalWeight,
        p.BirthDate,
        p.Gender
    );
}
