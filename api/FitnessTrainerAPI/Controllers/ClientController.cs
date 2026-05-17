using FitnessTrainerAPI.Data;
using FitnessTrainerAPI.DTOs;
using FitnessTrainerAPI.Models;
using FitnessTrainerAPI.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;

namespace FitnessTrainerAPI.Controllers;

[ApiController]
[Route("api/client")]
public class ClientController(AppDbContext db, IClientProfileService profileService) : ControllerBase
{
    // GET api/client/trainer — тренер текущего клиента
    [Authorize(Roles = "Client")]
    [HttpGet("trainer")]
    public async Task<IActionResult> GetMyTrainer()
    {
        var clientId = GetUserId();

        var profile = await db.ClientProfiles
            .Where(cp => cp.UserId == clientId && cp.TrainerId != null)
            .Include(cp => cp.Trainer)
            .FirstOrDefaultAsync();

        if (profile?.Trainer is null)
            return NotFound(new { message = "Тренер не назначен" });

        var t = profile.Trainer;
        return Ok(new
        {
            userId    = t.UserId,
            firstName = t.FirstName,
            lastName  = t.LastName,
            avatarUrl = t.AvatarUrl
        });
    }

    // POST api/client/profile — сохранить анкету (клиент заполняет при онбординге)
    [Authorize(Roles = "Client")]
    [HttpPost("profile")]
    public async Task<IActionResult> SaveProfile([FromBody] SaveClientProfileRequest request)
    {
        var userId = GetUserId();
        var result = await profileService.SaveProfileAsync(userId, request);
        if (result is null) return NotFound(new { message = "Профиль не найден" });
        return Ok(result);
    }

    // GET api/client/profile/{clientId} — профиль клиента (клиент или тренер)
    [Authorize]
    [HttpGet("profile/{clientId:int}")]
    public async Task<IActionResult> GetProfile(int clientId)
    {
        var result = await profileService.GetProfileAsync(clientId);
        if (result is null) return NotFound(new { message = "Профиль не найден" });
        return Ok(result);
    }

    // GET api/client/trainers — список всех тренеров для выбора
    [Authorize(Roles = "Client")]
    [HttpGet("trainers")]
    public async Task<IActionResult> GetTrainerList()
    {
        var trainers = await db.Users
            .Where(u => u.Role.RoleName == "Trainer" && u.IsActive)
            .Include(u => u.TrainerProfile)
            .Select(u => new
            {
                userId         = u.UserId,
                firstName      = u.FirstName,
                lastName       = u.LastName,
                avatarUrl      = u.AvatarUrl,
                specialization = u.TrainerProfile != null ? u.TrainerProfile.Specialization : null,
                experience     = u.TrainerProfile != null ? u.TrainerProfile.Experience : (int?)null,
                description    = u.TrainerProfile != null ? u.TrainerProfile.Description : null,
                rating         = db.TrainerReviews
                                    .Where(r => r.TrainerId == u.UserId)
                                    .Select(r => (double?)r.Rating)
                                    .Average(),
                reviewCount    = db.TrainerReviews.Count(r => r.TrainerId == u.UserId)
            })
            .ToListAsync();

        return Ok(trainers);
    }

    // POST api/client/select-trainer/{trainerId}
    [Authorize(Roles = "Client")]
    [HttpPost("select-trainer/{trainerId:int}")]
    public async Task<IActionResult> SelectTrainer(int trainerId)
    {
        var clientId = GetUserId();

        var trainerExists = await db.Users
            .AnyAsync(u => u.UserId == trainerId && u.Role.RoleName == "Trainer");
        if (!trainerExists)
            return NotFound(new { message = "Тренер не найден" });

        var profile = await db.ClientProfiles.FirstOrDefaultAsync(cp => cp.UserId == clientId);
        if (profile is null)
        {
            db.ClientProfiles.Add(new ClientProfile { UserId = clientId, TrainerId = trainerId });
        }
        else
        {
            profile.TrainerId = trainerId;
        }

        await db.SaveChangesAsync();
        return Ok(new { message = "Тренер выбран" });
    }

    // POST api/client/select-trainer-by-code/{code}
    [Authorize(Roles = "Client")]
    [HttpPost("select-trainer-by-code/{code}")]
    public async Task<IActionResult> SelectTrainerByCode(string code)
    {
        var clientId = GetUserId();

        var trainerProfile = await db.TrainerProfiles
            .Include(tp => tp.User)
            .FirstOrDefaultAsync(tp => tp.TrainerCode == code.ToUpper() && tp.User.IsActive);

        if (trainerProfile is null)
            return NotFound(new { message = "Тренер с таким кодом не найден" });

        var profile = await db.ClientProfiles.FirstOrDefaultAsync(cp => cp.UserId == clientId);
        if (profile is null)
            db.ClientProfiles.Add(new ClientProfile { UserId = clientId, TrainerId = trainerProfile.UserId });
        else
            profile.TrainerId = trainerProfile.UserId;

        await db.SaveChangesAsync();
        return Ok(new { message = "Тренер выбран", trainerId = trainerProfile.UserId });
    }

    private int GetUserId() =>
        int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
}
