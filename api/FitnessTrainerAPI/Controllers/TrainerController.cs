using FitnessTrainerAPI.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;

namespace FitnessTrainerAPI.Controllers;

[ApiController]
[Route("api/trainer")]
[Authorize(Roles = "Trainer")]
public class TrainerController(AppDbContext db) : ControllerBase
{
    // GET api/trainer/clients — список клиентов тренера
    [HttpGet("clients")]
    public async Task<IActionResult> GetClients()
    {
        var trainerId = GetUserId();

        var clients = await db.ClientProfiles
            .Where(cp => cp.TrainerId == trainerId)
            .Include(cp => cp.User)
            .Select(cp => new
            {
                userId    = cp.UserId,
                firstName = cp.User.FirstName,
                lastName  = cp.User.LastName,
                email     = cp.User.Email,
                phone     = cp.User.Phone,
                avatarUrl = cp.User.AvatarUrl
            })
            .ToListAsync();

        return Ok(clients);
    }

    // POST api/trainer/clients/{clientId} — привязать клиента к тренеру
    [HttpPost("clients/{clientId:int}")]
    public async Task<IActionResult> AssignClient(int clientId)
    {
        var trainerId = GetUserId();
        var profile = await db.ClientProfiles.FirstOrDefaultAsync(cp => cp.UserId == clientId);
        if (profile is null) return NotFound(new { message = "Клиент не найден" });
        if (profile.TrainerId is not null) return Conflict(new { message = "Клиент уже привязан к тренеру" });
        profile.TrainerId = trainerId;
        await db.SaveChangesAsync();
        return Ok(new { message = "Клиент привязан" });
    }

    // GET api/trainer/my-code — код тренера для отображения клиентам
    [HttpGet("my-code")]
    public async Task<IActionResult> GetMyCode()
    {
        var trainerId = GetUserId();
        var profile = await db.TrainerProfiles.FirstOrDefaultAsync(tp => tp.UserId == trainerId);
        if (profile is null) return NotFound(new { message = "Профиль тренера не найден" });
        return Ok(new { trainerCode = profile.TrainerCode });
    }

    private int GetUserId() =>
        int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
}
