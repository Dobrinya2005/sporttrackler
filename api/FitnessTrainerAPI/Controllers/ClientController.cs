using FitnessTrainerAPI.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;

namespace FitnessTrainerAPI.Controllers;

[ApiController]
[Route("api/client")]
[Authorize(Roles = "Client")]
public class ClientController(AppDbContext db) : ControllerBase
{
    // GET api/client/trainer — тренер текущего клиента
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

    private int GetUserId() =>
        int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
}
