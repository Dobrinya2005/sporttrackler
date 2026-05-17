using FitnessTrainerAPI.Data;
using FitnessTrainerAPI.DTOs;
using FitnessTrainerAPI.Models;
using Microsoft.EntityFrameworkCore;

namespace FitnessTrainerAPI.Services;

public interface IFoodService
{
    Task<List<FoodProductResponse>> SearchProductsAsync(string query);
    Task<FoodProductResponse> CreateCustomProductAsync(int userId, FoodProductCreateRequest request);
    Task<DiaryEntryResponse> AddDiaryEntryAsync(int clientId, DiaryEntryCreateRequest request);
    Task<DailySummaryResponse> GetDailySummaryAsync(int clientId, DateOnly date);
    Task<bool> DeleteDiaryEntryAsync(int diaryId, int clientId);
}

public class FoodService(AppDbContext db, IClientProfileService profileService) : IFoodService
{
    public async Task<List<FoodProductResponse>> SearchProductsAsync(string query)
    {
        return await db.FoodProducts
            .Where(p => p.Name.Contains(query) || (p.Brand != null && p.Brand.Contains(query)))
            .OrderBy(p => p.Name)
            .Take(30)
            .Select(p => ToProductResponse(p))
            .ToListAsync();
    }

    public async Task<FoodProductResponse> CreateCustomProductAsync(int userId, FoodProductCreateRequest req)
    {
        var product = new FoodProduct
        {
            Name            = req.Name,
            Brand           = req.Brand,
            CaloriesPer100g = req.CaloriesPer100g,
            ProteinPer100g  = req.ProteinPer100g,
            FatPer100g      = req.FatPer100g,
            CarbsPer100g    = req.CarbsPer100g,
            FiberPer100g    = req.FiberPer100g,
            IsCustom        = true,
            CreatedBy       = userId
        };
        db.FoodProducts.Add(product);
        await db.SaveChangesAsync();
        return ToProductResponse(product);
    }

    public async Task<DiaryEntryResponse> AddDiaryEntryAsync(int clientId, DiaryEntryCreateRequest req)
    {
        var product = await db.FoodProducts.FindAsync(req.ProductId)
            ?? throw new KeyNotFoundException("Product not found");

        var entry = new FoodDiaryEntry
        {
            ClientId  = clientId,
            DiaryDate = req.DiaryDate ?? DateOnly.FromDateTime(DateTime.UtcNow),
            MealType  = req.MealType,
            ProductId = req.ProductId,
            WeightG   = req.WeightG
        };
        db.FoodDiary.Add(entry);
        await db.SaveChangesAsync();

        return ToDiaryResponse(entry, product);
    }

    public async Task<DailySummaryResponse> GetDailySummaryAsync(int clientId, DateOnly date)
    {
        var profile = await db.ClientProfiles.FirstOrDefaultAsync(cp => cp.UserId == clientId);
        if (profile is not null && profile.DailyCalorieGoal is null)
        {
            try
            {
                profileService.RecalculateMacros(profile);
                await db.SaveChangesAsync();
            }
            catch { /* non-critical */ }
        }

        var entries = await db.FoodDiary
            .Include(d => d.Product)
            .Where(d => d.ClientId == clientId && d.DiaryDate == date)
            .ToListAsync();

        var meals = entries
            .GroupBy(e => e.MealType)
            .Select(g => new MealSummary(
                g.Key,
                Math.Round(g.Sum(e => e.WeightG / 100m * e.Product.CaloriesPer100g), 1),
                Math.Round(g.Sum(e => e.WeightG / 100m * e.Product.ProteinPer100g),  1),
                Math.Round(g.Sum(e => e.WeightG / 100m * e.Product.FatPer100g),      1),
                Math.Round(g.Sum(e => e.WeightG / 100m * e.Product.CarbsPer100g),    1),
                g.Select(e => ToDiaryResponse(e, e.Product)).ToList()
            ))
            .ToList();

        return new DailySummaryResponse(
            date,
            profile?.DailyCalorieGoal,
            profile?.DailyProteinGoal,
            profile?.DailyFatGoal,
            profile?.DailyCarbGoal,
            meals.Sum(m => m.Calories),
            meals.Sum(m => m.Protein),
            meals.Sum(m => m.Fat),
            meals.Sum(m => m.Carbs),
            meals
        );
    }

    public async Task<bool> DeleteDiaryEntryAsync(int diaryId, int clientId)
    {
        var entry = await db.FoodDiary.FindAsync(diaryId);
        if (entry is null || entry.ClientId != clientId) return false;
        db.FoodDiary.Remove(entry);
        await db.SaveChangesAsync();
        return true;
    }

    private static FoodProductResponse ToProductResponse(FoodProduct p) => new(
        p.ProductId, p.ExternalId, p.Name, p.Brand,
        p.CaloriesPer100g, p.ProteinPer100g, p.FatPer100g,
        p.CarbsPer100g, p.FiberPer100g, p.IsCustom);

    private static DiaryEntryResponse ToDiaryResponse(FoodDiaryEntry e, FoodProduct p) => new(
        e.DiaryId, e.DiaryDate, e.MealType,
        ToProductResponse(p),
        e.WeightG,
        Math.Round(e.WeightG / 100m * p.CaloriesPer100g, 1),
        Math.Round(e.WeightG / 100m * p.ProteinPer100g,  1),
        Math.Round(e.WeightG / 100m * p.FatPer100g,      1),
        Math.Round(e.WeightG / 100m * p.CarbsPer100g,    1));
}
