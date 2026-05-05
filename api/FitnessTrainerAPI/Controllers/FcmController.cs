using FitnessTrainerAPI.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System.Security.Claims;

namespace FitnessTrainerAPI.Controllers;

[ApiController]
[Route("api/fcm")]
[Authorize]
public class FcmController(IFcmService fcmService) : ControllerBase
{
    // POST api/fcm/token  { "deviceToken": "..." }
    [HttpPost("token")]
    public async Task<IActionResult> RegisterToken([FromBody] RegisterFcmTokenRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.DeviceToken))
            return BadRequest(new { message = "Токен устройства не может быть пустым" });

        var userId = int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
        await fcmService.RegisterTokenAsync(userId, request.DeviceToken);
        return Ok(new { message = "Токен зарегистрирован" });
    }

    public record RegisterFcmTokenRequest(string DeviceToken);
}
