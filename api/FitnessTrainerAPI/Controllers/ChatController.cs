using FitnessTrainerAPI.Hubs;
using FitnessTrainerAPI.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.SignalR;
using System.Security.Claims;

namespace FitnessTrainerAPI.Controllers;

[ApiController]
[Route("api/messages")]
[Authorize]
public class ChatController(IChatService chatService, IWebHostEnvironment env, IHubContext<ChatHub> hubContext, IFcmService fcmService) : ControllerBase
{
    [HttpGet("conversations")]
    public async Task<IActionResult> GetConversations()
    {
        return Ok(await chatService.GetConversationListAsync(GetUserId()));
    }

    [HttpGet("{contactId:int}")]
    public async Task<IActionResult> GetConversation(
        int contactId,
        [FromQuery] int page = 1,
        [FromQuery] int pageSize = 50)
    {
        return Ok(await chatService.GetConversationAsync(GetUserId(), contactId, page, pageSize));
    }

    [HttpPost("send")]
    public async Task<IActionResult> Send([FromBody] SendMessageRequest request)
    {
        var senderId = GetUserId();
        var result   = await chatService.SendMessageAsync(senderId, request);

        // Отправить получателю через SignalR в реальном времени
        await hubContext.Clients.Group($"user_{request.ReceiverId}")
            .SendAsync("ReceiveMessage", result);

        // FCM пуш если получатель офлайн (SignalR не доставит)
        var isOnline = ChatHub.IsOnline(request.ReceiverId);
        if (!isOnline)
        {
            var preview = request.Text ?? (request.AttachmentType switch {
                "audio"      => "🎤 Голосовое сообщение",
                "video_note" => "🎥 Видеосообщение",
                "image"      => "🖼 Фото",
                "video"      => "🎬 Видео",
                _            => "📎 Файл"
            });
            await fcmService.SendMessageNotificationAsync(request.ReceiverId, result.SenderName, preview);
        }

        return Ok(result);
    }

    [HttpPost("{senderId:int}/read")]
    public async Task<IActionResult> MarkRead(int senderId)
    {
        await chatService.MarkAsReadAsync(GetUserId(), senderId);
        return NoContent();
    }

    [HttpPost("upload-media")]
    [Consumes("multipart/form-data")]
    public async Task<IActionResult> UploadMedia(IFormFile file)
    {
        if (file is null || file.Length == 0)
            return BadRequest(new { message = "Файл не выбран" });

        var ext = Path.GetExtension(file.FileName).ToLowerInvariant();

        string attachmentType = ext switch {
            ".jpg" or ".jpeg" or ".png" or ".webp" or ".gif" => "image",
            ".mp4" or ".mov" or ".avi" or ".mkv"             => "video",
            ".m4a" or ".aac" or ".mp3" or ".ogg" or ".wav"  => "audio",
            _                                                => "file"
        };

        var uploadsDir = Path.Combine(env.WebRootPath ?? "wwwroot", "chat-media");
        Directory.CreateDirectory(uploadsDir);

        var userId   = GetUserId();
        var fileName = $"chat_{userId}_{DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()}{ext}";
        var filePath = Path.Combine(uploadsDir, fileName);

        using (var stream = new FileStream(filePath, FileMode.Create))
            await file.CopyToAsync(stream);

        var baseUrl     = $"{Request.Scheme}://{Request.Host}";
        var mediaUrl    = $"{baseUrl}/chat-media/{fileName}";

        return Ok(new { mediaUrl, attachmentType, fileName = file.FileName });
    }

    private int GetUserId() =>
        int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
}
