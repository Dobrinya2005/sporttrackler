using System.Text.Json;
using Microsoft.AspNetCore.Hosting;
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
[Route("api/auth")]
public class AuthController(IAuthService authService, AppDbContext db, IEmailService emailService, IConfiguration config, IWebHostEnvironment env) : ControllerBase
{
    [HttpPost("register")]
    public async Task<IActionResult> Register([FromBody] RegisterRequest request)
    {
        if (request.Role == "Trainer")
        {
            var expectedCode = config["TrainerInviteCode"] ?? "SPORT2026";
            if (string.IsNullOrWhiteSpace(request.TrainerCode) || request.TrainerCode != expectedCode)
                return BadRequest(new { message = "Неверный код тренера. Обратитесь к администратору." });
        }

        if (await db.Users.AnyAsync(u => u.Email == request.Email))
            return BadRequest(new { message = "Email уже зарегистрирован" });

        if (await db.Roles.AllAsync(r => r.RoleName != request.Role))
            return BadRequest(new { message = "Неверная роль" });

        // Инвалидируем старые коды для этого email
        var old = await db.EmailVerificationCodes
            .Where(c => c.Email == request.Email && !c.IsUsed)
            .ToListAsync();
        old.ForEach(c => c.IsUsed = true);

        var code = new string(Enumerable.Range(0, 6)
            .Select(_ => (char)('0' + Random.Shared.Next(10))).ToArray());

        db.EmailVerificationCodes.Add(new EmailVerificationCode
        {
            Email            = request.Email,
            Code             = code,
            ExpiresAt        = DateTime.UtcNow.AddMinutes(10),
            RegistrationData = JsonSerializer.Serialize(request)
        });
        await db.SaveChangesAsync();

        _ = Task.Run(() => emailService.SendVerificationCodeAsync(request.Email, code));
        if (env.IsDevelopment())
            return Ok(new { message = "Код подтверждения отправлен на email", devCode = code });
        return Ok(new { message = "Код подтверждения отправлен на email", devCode = code });
    }

    [HttpPost("login")]
    public async Task<IActionResult> Login([FromBody] LoginRequest request)
    {
        var result = await authService.LoginAsync(request);
        if (result is null) return Unauthorized(new { message = "Неверный email или пароль" });
        return Ok(result);
    }

    [HttpPost("refresh")]
    public async Task<IActionResult> Refresh([FromBody] RefreshTokenRequest request)
    {
        var result = await authService.RefreshTokenAsync(request.RefreshToken);
        if (result is null) return Unauthorized(new { message = "Токен недействителен или истёк" });
        return Ok(result);
    }

    [Authorize]
    [HttpPost("logout")]
    public async Task<IActionResult> Logout()
    {
        var userId = int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
        await authService.RevokeTokenAsync(userId);
        return Ok(new { message = "Выход выполнен" });
    }

    [HttpPost("send-code")]
    public async Task<IActionResult> SendCode([FromBody] SendCodeRequest request)
    {
        var old = await db.EmailVerificationCodes
            .Where(c => c.Email == request.Email && !c.IsUsed)
            .ToListAsync();
        old.ForEach(c => c.IsUsed = true);

        var code = new string(Enumerable.Range(0, 6)
            .Select(_ => (char)('0' + Random.Shared.Next(10))).ToArray());

        db.EmailVerificationCodes.Add(new EmailVerificationCode
        {
            Email     = request.Email,
            Code      = code,
            ExpiresAt = DateTime.UtcNow.AddMinutes(10)
        });
        await db.SaveChangesAsync();

        _ = Task.Run(() => emailService.SendVerificationCodeAsync(request.Email, code));
        return Ok(new { message = "Код отправлен" });
    }

    [HttpPost("forgot-password")]
    public async Task<IActionResult> ForgotPassword([FromBody] SendCodeRequest request)
    {
        var userExists = await db.Users.AnyAsync(u => u.Email == request.Email);
        if (!userExists)
            return BadRequest(new { message = "Пользователь с таким email не найден" });

        var old = await db.EmailVerificationCodes
            .Where(c => c.Email == request.Email && !c.IsUsed)
            .ToListAsync();
        old.ForEach(c => c.IsUsed = true);

        var code = new string(Enumerable.Range(0, 6)
            .Select(_ => (char)('0' + Random.Shared.Next(10))).ToArray());

        db.EmailVerificationCodes.Add(new EmailVerificationCode
        {
            Email     = request.Email,
            Code      = code,
            ExpiresAt = DateTime.UtcNow.AddMinutes(10)
        });
        await db.SaveChangesAsync();

        _ = Task.Run(() => emailService.SendVerificationCodeAsync(request.Email, code,
            subject: "Сброс пароля SportTrackler",
            heading: "Ваш код для сброса пароля:"));
        return Ok(new { message = "Код отправлен на email" });
    }

    [HttpPost("reset-password")]
    public async Task<IActionResult> ResetPassword([FromBody] ResetPasswordRequest request)
    {
        var entry = await db.EmailVerificationCodes
            .Where(c => c.Email == request.Email && c.Code == request.Code && !c.IsUsed
                        && string.IsNullOrEmpty(c.RegistrationData))
            .OrderByDescending(c => c.CreatedAt)
            .FirstOrDefaultAsync();

        if (entry is null)
            return BadRequest(new { message = "Неверный код" });

        if (entry.ExpiresAt < DateTime.UtcNow)
            return BadRequest(new { message = "Код истёк" });

        var user = await db.Users.FirstOrDefaultAsync(u => u.Email == request.Email);
        if (user is null)
            return BadRequest(new { message = "Пользователь не найден" });

        user.PasswordHash = BCrypt.Net.BCrypt.HashPassword(request.NewPassword);
        entry.IsUsed = true;
        await db.SaveChangesAsync();

        return Ok(new { message = "Пароль успешно изменён" });
    }

    [HttpPost("verify-code")]
    public async Task<IActionResult> VerifyCode([FromBody] VerifyCodeRequest request)
    {
        var entry = await db.EmailVerificationCodes
            .Where(c => c.Email == request.Email && c.Code == request.Code && !c.IsUsed)
            .OrderByDescending(c => c.CreatedAt)
            .FirstOrDefaultAsync();

        if (entry is null)
            return BadRequest(new { message = "Неверный код" });

        if (entry.ExpiresAt < DateTime.UtcNow)
            return BadRequest(new { message = "Код истёк" });

        entry.IsUsed = true;
        await db.SaveChangesAsync();

        // Если это регистрационная верификация — создаём пользователя
        if (!string.IsNullOrEmpty(entry.RegistrationData))
        {
            var regRequest = JsonSerializer.Deserialize<RegisterRequest>(entry.RegistrationData);
            if (regRequest is null)
                return StatusCode(500, new { message = "Ошибка данных регистрации" });

            var result = await authService.RegisterAsync(regRequest);
            if (result is null)
                return BadRequest(new { message = "Не удалось создать аккаунт" });

            return Ok(result);
        }

        return Ok(new { message = "Email подтверждён" });
    }
}
