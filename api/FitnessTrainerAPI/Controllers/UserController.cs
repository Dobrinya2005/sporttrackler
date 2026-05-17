using FitnessTrainerAPI.Data;
using FitnessTrainerAPI.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;

namespace FitnessTrainerAPI.Controllers;

[ApiController]
[Route("api/user")]
[Authorize]
public class UserController(AppDbContext db, IWebHostEnvironment env) : ControllerBase
{
    // GET api/user/me — текущий пользователь
    [HttpGet("me")]
    public async Task<IActionResult> GetMe()
    {
        var userId = GetUserId();
        var user = await db.Users
            .Include(u => u.Role)
            .FirstOrDefaultAsync(u => u.UserId == userId);

        if (user is null) return NotFound();

        return Ok(new
        {
            userId    = user.UserId,
            firstName = user.FirstName,
            lastName  = user.LastName,
            email     = user.Email,
            phone     = user.Phone,
            role      = user.Role.RoleName,
            avatarUrl = user.AvatarUrl
        });
    }

    // POST api/user/avatar — загрузка аватарки
    [HttpPost("avatar")]
    public async Task<IActionResult> UploadAvatar(IFormFile file)
    {
        if (file is null || file.Length == 0)
            return BadRequest(new { message = "Файл не выбран" });

        var allowed = new[] { ".jpg", ".jpeg", ".png", ".webp" };
        var ext = Path.GetExtension(file.FileName).ToLowerInvariant();
        if (!allowed.Contains(ext))
            return BadRequest(new { message = "Допустимые форматы: jpg, png, webp" });

        var userId = GetUserId();
        var uploadsDir = Path.Combine(env.WebRootPath ?? "wwwroot", "avatars");
        Directory.CreateDirectory(uploadsDir);

        var fileName  = $"avatar_{userId}_{DateTimeOffset.UtcNow.ToUnixTimeSeconds()}{ext}";
        var filePath  = Path.Combine(uploadsDir, fileName);

        using (var stream = new FileStream(filePath, FileMode.Create))
            await file.CopyToAsync(stream);

        var baseUrl   = $"{Request.Scheme}://{Request.Host}";
        var avatarUrl = $"{baseUrl}/avatars/{fileName}";

        var user = await db.Users.FindAsync(userId);
        if (user is null) return NotFound();
        user.AvatarUrl = avatarUrl;
        await db.SaveChangesAsync();

        return Ok(new { avatarUrl });
    }

    // PUT api/user/phone — обновить телефон
    [HttpPut("phone")]
    public async Task<IActionResult> UpdatePhone([FromBody] UpdatePhoneRequest req)
    {
        var userId = GetUserId();
        var user = await db.Users.FindAsync(userId);
        if (user is null) return NotFound();
        user.Phone = req.Phone;
        await db.SaveChangesAsync();
        return Ok(new { message = "Телефон обновлён" });
    }

    private int GetUserId() =>
        int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
}

public class UpdatePhoneRequest { public string? Phone { get; set; } }
