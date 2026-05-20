using FitnessTrainerAPI.Data;
using FitnessTrainerAPI.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;

namespace FitnessTrainerAPI.Controllers;

[ApiController]
[Route("api/admin")]
[Authorize(Roles = "Admin")]
public class AdminController(AppDbContext db) : ControllerBase
{
    // GET api/admin/trainers
    [HttpGet("trainers")]
    public async Task<IActionResult> GetTrainers()
    {
        var trainers = await db.TrainerProfiles
            .Include(tp => tp.User)
            .Select(tp => new
            {
                userId      = tp.UserId,
                firstName   = tp.User.FirstName,
                lastName    = tp.User.LastName,
                email       = tp.User.Email,
                phone       = tp.User.Phone,
                avatarUrl   = tp.User.AvatarUrl,
                isActive    = tp.User.IsActive,
                trainerCode = tp.TrainerCode,
                createdAt   = tp.User.CreatedAt,
                clientCount = db.ClientProfiles.Count(cp => cp.TrainerId == tp.UserId),
                reviewCount = db.TrainerReviews.Count(r => r.TrainerId == tp.UserId),
                avgRating   = db.TrainerReviews
                    .Where(r => r.TrainerId == tp.UserId)
                    .Select(r => (double?)r.Rating)
                    .Average()
            })
            .OrderByDescending(t => t.createdAt)
            .ToListAsync();

        return Ok(trainers);
    }

    // GET api/admin/trainers/{id}
    [HttpGet("trainers/{id:int}")]
    public async Task<IActionResult> GetTrainer(int id)
    {
        var tp = await db.TrainerProfiles
            .Include(t => t.User)
            .FirstOrDefaultAsync(t => t.UserId == id);

        if (tp is null) return NotFound(new { message = "Тренер не найден" });

        return Ok(new
        {
            userId         = tp.UserId,
            firstName      = tp.User.FirstName,
            lastName       = tp.User.LastName,
            email          = tp.User.Email,
            phone          = tp.User.Phone,
            avatarUrl      = tp.User.AvatarUrl,
            isActive       = tp.User.IsActive,
            trainerCode    = tp.TrainerCode,
            specialization = tp.Specialization,
            experience     = tp.Experience,
            description    = tp.Description,
            createdAt      = tp.User.CreatedAt,
            clientCount    = await db.ClientProfiles.CountAsync(cp => cp.TrainerId == id)
        });
    }

    // POST api/admin/trainers — создать тренера вручную
    [HttpPost("trainers")]
    public async Task<IActionResult> CreateTrainer([FromBody] CreateTrainerRequest req)
    {
        if (await db.Users.AnyAsync(u => u.Email == req.Email))
            return Conflict(new { message = "Email уже занят" });

        var trainerRole = await db.Roles.FirstAsync(r => r.RoleName == "Trainer");

        var user = new User
        {
            RoleId       = trainerRole.RoleId,
            FirstName    = req.FirstName,
            LastName     = req.LastName,
            Email        = req.Email,
            PasswordHash = BCrypt.Net.BCrypt.HashPassword(req.Password),
            IsActive     = true
        };
        db.Users.Add(user);
        await db.SaveChangesAsync();

        db.TrainerProfiles.Add(new TrainerProfile
        {
            UserId      = user.UserId,
            TrainerCode = GenerateTrainerCode()
        });
        await db.SaveChangesAsync();

        return Ok(new { message = "Тренер создан", userId = user.UserId });
    }

    // PUT api/admin/trainers/{id}/block
    [HttpPut("trainers/{id:int}/block")]
    public async Task<IActionResult> ToggleBlock(int id)
    {
        var user = await db.Users.FindAsync(id);
        if (user is null || user.Role?.RoleName != "Trainer")
        {
            // load role
            var u = await db.Users.Include(u => u.Role).FirstOrDefaultAsync(u => u.UserId == id);
            if (u is null || u.Role.RoleName != "Trainer")
                return NotFound(new { message = "Тренер не найден" });
            u.IsActive = !u.IsActive;
            await db.SaveChangesAsync();
            return Ok(new { isActive = u.IsActive });
        }
        user.IsActive = !user.IsActive;
        await db.SaveChangesAsync();
        return Ok(new { isActive = user.IsActive });
    }

    // DELETE api/admin/trainers/{id}
    [HttpDelete("trainers/{id:int}")]
    public async Task<IActionResult> DeleteTrainer(int id)
    {
        var user = await db.Users
            .Include(u => u.Role)
            .Include(u => u.TrainerProfile)
            .FirstOrDefaultAsync(u => u.UserId == id);

        if (user is null || user.Role.RoleName != "Trainer")
            return NotFound(new { message = "Тренер не найден" });

        // Отвязываем клиентов от этого тренера
        var clientProfiles = await db.ClientProfiles.Where(cp => cp.TrainerId == id).ToListAsync();
        foreach (var c in clientProfiles) c.TrainerId = null;
        await db.SaveChangesAsync();

        // Удаляем отзывы о тренере
        db.TrainerReviews.RemoveRange(await db.TrainerReviews.Where(r => r.TrainerId == id).ToListAsync());

        // Удаляем планы тренировок (cascade удалит WorkoutDay→Exercise)
        db.WorkoutPlans.RemoveRange(await db.WorkoutPlans.Where(wp => wp.TrainerId == id).ToListAsync());

        // Удаляем созданные тренером группы (cascade удалит GroupMembers+GroupMessages)
        db.ChatGroups.RemoveRange(await db.ChatGroups.Where(g => g.CreatedBy == id).ToListAsync());

        // Удаляем участие в чужих группах и сообщения в них
        db.GroupMessages.RemoveRange(await db.GroupMessages.Where(gm => gm.SenderId == id).ToListAsync());
        db.GroupMembers.RemoveRange(await db.GroupMembers.Where(gm => gm.UserId == id).ToListAsync());

        // Реакции на сообщения
        db.MessageReactions.RemoveRange(await db.MessageReactions.Where(r => r.UserId == id).ToListAsync());

        // Личные сообщения (сначала реакции на них)
        var msgIds = await db.Messages.Where(m => m.SenderId == id || m.ReceiverId == id).Select(m => m.MessageId).ToListAsync();
        db.MessageReactions.RemoveRange(await db.MessageReactions.Where(r => msgIds.Contains(r.MessageId)).ToListAsync());
        db.Messages.RemoveRange(await db.Messages.Where(m => msgIds.Contains(m.MessageId)).ToListAsync());

        // Уведомления, токены
        db.Notifications.RemoveRange(await db.Notifications.Where(n => n.UserId == id).ToListAsync());
        db.FcmTokens.RemoveRange(await db.FcmTokens.Where(t => t.UserId == id).ToListAsync());
        db.RefreshTokens.RemoveRange(await db.RefreshTokens.Where(t => t.UserId == id).ToListAsync());

        db.Users.Remove(user);
        await db.SaveChangesAsync();
        return Ok(new { message = "Тренер удалён" });
    }

    // GET api/admin/clients
    [HttpGet("clients")]
    public async Task<IActionResult> GetClients()
    {
        var clientRole = await db.Roles.FirstAsync(r => r.RoleName == "Client");
        var clients = await db.ClientProfiles
            .Include(cp => cp.User)
            .Include(cp => cp.Trainer)
            .Select(cp => new
            {
                userId      = cp.UserId,
                firstName   = cp.User.FirstName,
                lastName    = cp.User.LastName,
                email       = cp.User.Email,
                phone       = cp.User.Phone,
                avatarUrl   = cp.User.AvatarUrl,
                isActive    = cp.User.IsActive,
                createdAt   = cp.User.CreatedAt,
                trainerName = cp.Trainer != null
                    ? cp.Trainer.FirstName + " " + cp.Trainer.LastName
                    : null,
                trainerId   = (int?)cp.TrainerId
            })
            .OrderByDescending(c => c.createdAt)
            .ToListAsync();
        return Ok(clients);
    }

    // PUT api/admin/clients/{id}/block
    [HttpPut("clients/{id:int}/block")]
    public async Task<IActionResult> ToggleClientBlock(int id)
    {
        var u = await db.Users.Include(u => u.Role).FirstOrDefaultAsync(u => u.UserId == id);
        if (u is null || u.Role.RoleName != "Client")
            return NotFound(new { message = "Клиент не найден" });
        u.IsActive = !u.IsActive;
        await db.SaveChangesAsync();
        return Ok(new { isActive = u.IsActive });
    }

    // DELETE api/admin/clients/{id}
    [HttpDelete("clients/{id:int}")]
    public async Task<IActionResult> DeleteClient(int id)
    {
        var user = await db.Users
            .Include(u => u.Role)
            .Include(u => u.ClientProfile)
            .FirstOrDefaultAsync(u => u.UserId == id);
        if (user is null || user.Role.RoleName != "Client")
            return NotFound(new { message = "Клиент не найден" });

        // Отзывы клиента
        db.TrainerReviews.RemoveRange(await db.TrainerReviews.Where(r => r.ClientId == id).ToListAsync());

        // Планы тренировок (cascade удалит WorkoutDay→Exercise)
        db.WorkoutPlans.RemoveRange(await db.WorkoutPlans.Where(wp => wp.ClientId == id).ToListAsync());

        // Логи тренировок (cascade удалит ExerciseLog)
        db.WorkoutLogs.RemoveRange(await db.WorkoutLogs.Where(wl => wl.ClientId == id).ToListAsync());

        // Замеры и фото прогресса
        db.Measurements.RemoveRange(await db.Measurements.Where(m => m.ClientId == id).ToListAsync());
        db.ProgressPhotos.RemoveRange(await db.ProgressPhotos.Where(p => p.ClientId == id).ToListAsync());

        // Дневник питания
        db.FoodDiary.RemoveRange(await db.FoodDiary.Where(f => f.ClientId == id).ToListAsync());

        // Созданные клиентом группы (cascade удалит членов и сообщения)
        db.ChatGroups.RemoveRange(await db.ChatGroups.Where(g => g.CreatedBy == id).ToListAsync());

        // Участие в чужих группах и сообщения
        db.GroupMessages.RemoveRange(await db.GroupMessages.Where(gm => gm.SenderId == id).ToListAsync());
        db.GroupMembers.RemoveRange(await db.GroupMembers.Where(gm => gm.UserId == id).ToListAsync());

        // Реакции и личные сообщения
        db.MessageReactions.RemoveRange(await db.MessageReactions.Where(r => r.UserId == id).ToListAsync());
        var msgIds = await db.Messages.Where(m => m.SenderId == id || m.ReceiverId == id).Select(m => m.MessageId).ToListAsync();
        db.MessageReactions.RemoveRange(await db.MessageReactions.Where(r => msgIds.Contains(r.MessageId)).ToListAsync());
        db.Messages.RemoveRange(await db.Messages.Where(m => msgIds.Contains(m.MessageId)).ToListAsync());

        // Уведомления, токены
        db.Notifications.RemoveRange(await db.Notifications.Where(n => n.UserId == id).ToListAsync());
        db.FcmTokens.RemoveRange(await db.FcmTokens.Where(t => t.UserId == id).ToListAsync());
        db.RefreshTokens.RemoveRange(await db.RefreshTokens.Where(t => t.UserId == id).ToListAsync());

        db.Users.Remove(user);
        await db.SaveChangesAsync();
        return Ok(new { message = "Клиент удалён" });
    }

    // GET api/admin/trainers/{id}/reviews
    [HttpGet("trainers/{id:int}/reviews")]
    public async Task<IActionResult> GetReviews(int id)
    {
        var reviews = await db.TrainerReviews
            .Where(r => r.TrainerId == id)
            .Include(r => r.Client)
            .OrderByDescending(r => r.CreatedAt)
            .Select(r => new
            {
                reviewId     = r.ReviewId,
                clientName   = r.Client.FirstName + " " + r.Client.LastName,
                avatarUrl    = r.Client.AvatarUrl,
                rating       = r.Rating,
                comment      = r.Comment,
                adminReply   = r.AdminReply,
                adminReplyAt = r.AdminReplyAt,
                createdAt    = r.CreatedAt
            })
            .ToListAsync();

        return Ok(reviews);
    }

    // DELETE api/admin/reviews/{id}
    [HttpDelete("reviews/{id:int}")]
    public async Task<IActionResult> DeleteReview(int id)
    {
        var review = await db.TrainerReviews.FindAsync(id);
        if (review is null) return NotFound(new { message = "Отзыв не найден" });
        db.TrainerReviews.Remove(review);
        await db.SaveChangesAsync();
        return Ok(new { message = "Отзыв удалён" });
    }

    // POST api/admin/reviews/{id}/reply
    [HttpPost("reviews/{id:int}/reply")]
    public async Task<IActionResult> ReplyReview(int id, [FromBody] ReplyRequest req)
    {
        var review = await db.TrainerReviews.FindAsync(id);
        if (review is null) return NotFound(new { message = "Отзыв не найден" });
        review.AdminReply   = req.Reply;
        review.AdminReplyAt = DateTime.UtcNow;
        await db.SaveChangesAsync();
        return Ok(new { message = "Ответ сохранён" });
    }

    private static string GenerateTrainerCode()
    {
        const string chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        var bytes = new byte[6];
        System.Security.Cryptography.RandomNumberGenerator.Fill(bytes);
        return new string(bytes.Select(b => chars[b % chars.Length]).ToArray());
    }

    private int GetUserId() =>
        int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
}

public record CreateTrainerRequest(string FirstName, string LastName, string Email, string Password);
public record ReplyRequest(string Reply);
