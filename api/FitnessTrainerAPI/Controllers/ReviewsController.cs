using FitnessTrainerAPI.Data;
using FitnessTrainerAPI.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;

namespace FitnessTrainerAPI.Controllers;

[ApiController]
[Route("api/reviews")]
[Authorize]
public class ReviewsController(AppDbContext db) : ControllerBase
{
    // POST api/reviews — клиент оставляет отзыв тренеру
    [Authorize(Roles = "Client")]
    [HttpPost]
    public async Task<IActionResult> AddReview([FromBody] AddReviewRequest request)
    {
        var clientId = GetUserId();

        if (request.Rating < 1 || request.Rating > 5)
            return BadRequest(new { message = "Рейтинг должен быть от 1 до 5" });

        var trainerExists = await db.Users
            .AnyAsync(u => u.UserId == request.TrainerId && u.Role.RoleName == "Trainer");
        if (!trainerExists)
            return NotFound(new { message = "Тренер не найден" });

        // один отзыв от клиента одному тренеру
        var existing = await db.TrainerReviews
            .FirstOrDefaultAsync(r => r.ClientId == clientId && r.TrainerId == request.TrainerId);

        if (existing is not null)
        {
            existing.Rating  = request.Rating;
            existing.Comment = request.Comment;
        }
        else
        {
            db.TrainerReviews.Add(new TrainerReview
            {
                ClientId  = clientId,
                TrainerId = request.TrainerId,
                Rating    = request.Rating,
                Comment   = request.Comment
            });
        }

        await db.SaveChangesAsync();
        return Ok(new { message = "Отзыв сохранён" });
    }

    // GET api/reviews/trainer/{trainerId}
    [HttpGet("trainer/{trainerId:int}")]
    public async Task<IActionResult> GetTrainerReviews(int trainerId)
    {
        var reviews = await db.TrainerReviews
            .Where(r => r.TrainerId == trainerId)
            .Include(r => r.Client)
            .OrderByDescending(r => r.CreatedAt)
            .Select(r => new
            {
                reviewId   = r.ReviewId,
                clientId   = r.ClientId,
                clientName = r.Client.FirstName + " " + r.Client.LastName,
                avatarUrl  = r.Client.AvatarUrl,
                rating     = r.Rating,
                comment    = r.Comment,
                createdAt  = r.CreatedAt
            })
            .ToListAsync();

        var avg = reviews.Count > 0 ? reviews.Average(r => r.rating) : 0.0;

        return Ok(new { averageRating = avg, reviews });
    }

    // GET api/reviews/my-review/{trainerId} — мой отзыв этому тренеру (если есть)
    [Authorize(Roles = "Client")]
    [HttpGet("my-review/{trainerId:int}")]
    public async Task<IActionResult> GetMyReview(int trainerId)
    {
        var clientId = GetUserId();
        var r = await db.TrainerReviews
            .FirstOrDefaultAsync(r => r.ClientId == clientId && r.TrainerId == trainerId);

        if (r is null) return NotFound();
        return Ok(new { r.Rating, r.Comment });
    }

    private int GetUserId() =>
        int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
}

public class AddReviewRequest
{
    public int TrainerId { get; set; }
    public int Rating    { get; set; }
    public string? Comment { get; set; }
}
