using FitnessTrainerAPI.Data;
using FitnessTrainerAPI.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;

namespace FitnessTrainerAPI.Controllers;

[ApiController]
[Route("api/photos")]
[Authorize]
public class PhotosController(AppDbContext db, IWebHostEnvironment env, IHttpContextAccessor http)
    : ControllerBase
{
    private static readonly string[] AllowedExtensions = [".jpg", ".jpeg", ".png", ".webp"];

    [HttpPost("upload")]
    public async Task<IActionResult> Upload(
        IFormFile file,
        [FromForm] string? poseType,
        [FromForm] string? description,
        [FromForm] bool isVisibleToTrainer = true)
    {
        if (file is null || file.Length == 0)
            return BadRequest(new { message = "Файл не выбран" });

        var ext = Path.GetExtension(file.FileName).ToLowerInvariant();
        if (!AllowedExtensions.Contains(ext))
            return BadRequest(new { message = "Допустимы только изображения (jpg, png, webp)" });

        var clientId = GetUserId();
        var fileName = $"{Guid.NewGuid()}{ext}";
        var folder   = Path.Combine(env.WebRootPath, "uploads", "photos", clientId.ToString());
        Directory.CreateDirectory(folder);

        var filePath = Path.Combine(folder, fileName);
        await using (var stream = System.IO.File.Create(filePath))
            await file.CopyToAsync(stream);

        var baseUrl  = $"{http.HttpContext!.Request.Scheme}://{http.HttpContext.Request.Host}";
        var photoUrl = $"{baseUrl}/uploads/photos/{clientId}/{fileName}";

        var photo = new ProgressPhoto
        {
            ClientId           = clientId,
            PhotoUrl           = photoUrl,
            PhotoDate          = DateOnly.FromDateTime(DateTime.UtcNow),
            PoseType           = poseType,
            Description        = description,
            IsVisibleToTrainer = isVisibleToTrainer
        };
        db.ProgressPhotos.Add(photo);
        await db.SaveChangesAsync();

        return Ok(new
        {
            photo.PhotoId,
            photo.PhotoUrl,
            photo.ThumbnailUrl,
            photo.PhotoDate,
            photo.PoseType,
            photo.Description,
            photo.IsVisibleToTrainer,
            photo.CreatedAt
        });
    }

    [HttpGet("{clientId:int}")]
    public async Task<IActionResult> GetPhotos(int clientId, [FromQuery] string? poseType)
    {
        if (!CanAccessClient(clientId)) return Forbid();

        var query = db.ProgressPhotos.Where(p => p.ClientId == clientId);

        if (User.IsInRole("Trainer"))
            query = query.Where(p => p.IsVisibleToTrainer);

        if (!string.IsNullOrEmpty(poseType))
            query = query.Where(p => p.PoseType == poseType);

        var photos = await query
            .OrderByDescending(p => p.PhotoDate)
            .Select(p => new
            {
                p.PhotoId,
                p.PhotoUrl,
                p.ThumbnailUrl,
                p.PhotoDate,
                p.PoseType,
                p.Description,
                p.IsVisibleToTrainer,
                p.CreatedAt
            })
            .ToListAsync();

        return Ok(photos);
    }

    [HttpDelete("{photoId:int}")]
    public async Task<IActionResult> Delete(int photoId)
    {
        var photo = await db.ProgressPhotos.FindAsync(photoId);
        if (photo is null || photo.ClientId != GetUserId()) return NotFound();

        // Удаляем физический файл
        try
        {
            var uri      = new Uri(photo.PhotoUrl);
            var filePath = Path.Combine(env.WebRootPath, uri.AbsolutePath.TrimStart('/').Replace('/', Path.DirectorySeparatorChar));
            if (System.IO.File.Exists(filePath))
                System.IO.File.Delete(filePath);
        }
        catch { /* игнорируем ошибки удаления файла */ }

        db.ProgressPhotos.Remove(photo);
        await db.SaveChangesAsync();
        return NoContent();
    }

    private int GetUserId() =>
        int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);

    private bool CanAccessClient(int clientId) =>
        GetUserId() == clientId || User.IsInRole("Trainer");
}
