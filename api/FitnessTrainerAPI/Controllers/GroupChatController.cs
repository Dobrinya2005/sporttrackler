using FitnessTrainerAPI.Hubs;
using FitnessTrainerAPI.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.SignalR;
using System.Security.Claims;

namespace FitnessTrainerAPI.Controllers;

[ApiController]
[Route("api/groups")]
[Authorize]
public class GroupChatController(
    IGroupChatService groupService,
    IHubContext<ChatHub> hubContext,
    IFcmService fcmService) : ControllerBase
{
    [HttpPost]
    public async Task<IActionResult> CreateGroup([FromBody] CreateGroupRequest request)
    {
        var userId = GetUserId();
        var group  = await groupService.CreateGroupAsync(userId, request);

        // Уведомить всех участников через SignalR
        foreach (var m in group.Members)
            await hubContext.Clients.Group($"user_{m.UserId}")
                .SendAsync("GroupCreated", group);

        return Ok(group);
    }

    [HttpGet]
    public async Task<IActionResult> GetMyGroups()
    {
        return Ok(await groupService.GetUserGroupsAsync(GetUserId()));
    }

    [HttpGet("{groupId:int}")]
    public async Task<IActionResult> GetGroup(int groupId)
    {
        var group = await groupService.GetGroupAsync(groupId, GetUserId());
        if (group is null) return NotFound();
        return Ok(group);
    }

    [HttpGet("{groupId:int}/messages")]
    public async Task<IActionResult> GetMessages(
        int groupId,
        [FromQuery] int page = 1,
        [FromQuery] int pageSize = 50)
    {
        try
        {
            var msgs = await groupService.GetGroupMessagesAsync(groupId, GetUserId(), page, pageSize);
            return Ok(msgs);
        }
        catch (UnauthorizedAccessException) { return Forbid(); }
    }

    [HttpPost("{groupId:int}/messages")]
    public async Task<IActionResult> SendMessage(int groupId, [FromBody] SendGroupMessageBody body)
    {
        var senderId = GetUserId();
        try
        {
            var request = new SendGroupMessageRequest(groupId, body.Text, body.AttachmentUrl, body.AttachmentType);
            var msg     = await groupService.SendMessageAsync(senderId, request);

            // Разослать всем участникам через SignalR
            await hubContext.Clients.Group($"group_{groupId}")
                .SendAsync("ReceiveGroupMessage", msg);

            // FCM офлайн-участникам
            var memberIds = await groupService.GetMemberIdsAsync(groupId);
            var preview   = body.Text ?? "📎 Вложение";
            foreach (var memberId in memberIds.Where(id => id != senderId && !ChatHub.IsOnline(id)))
                await fcmService.SendMessageNotificationAsync(memberId, senderId, msg.SenderName, $"[{msg.SenderName.Split(' ')[0]}] {preview}");

            return Ok(msg);
        }
        catch (UnauthorizedAccessException) { return Forbid(); }
    }

    [HttpPost("{groupId:int}/members/{userId:int}")]
    public async Task<IActionResult> AddMember(int groupId, int userId)
    {
        try
        {
            await groupService.AddMemberAsync(groupId, GetUserId(), userId);
            var group = await groupService.GetGroupAsync(groupId, GetUserId());
            await hubContext.Clients.Group($"user_{userId}").SendAsync("AddedToGroup", group);
            await hubContext.Clients.Group($"group_{groupId}").SendAsync("GroupUpdated", group);
            return NoContent();
        }
        catch (UnauthorizedAccessException) { return Forbid(); }
    }

    [HttpDelete("{groupId:int}/members/{userId:int}")]
    public async Task<IActionResult> RemoveMember(int groupId, int userId)
    {
        try
        {
            await groupService.RemoveMemberAsync(groupId, GetUserId(), userId);
            var group = await groupService.GetGroupAsync(groupId, GetUserId());
            await hubContext.Clients.Group($"group_{groupId}").SendAsync("GroupUpdated", group);
            return NoContent();
        }
        catch (UnauthorizedAccessException) { return Forbid(); }
    }

    [HttpPost("upload-avatar")]
    [Consumes("multipart/form-data")]
    public async Task<IActionResult> UploadAvatar(IFormFile file, [FromServices] IWebHostEnvironment env)
    {
        if (file is null || file.Length == 0) return BadRequest();
        var ext      = Path.GetExtension(file.FileName).ToLowerInvariant();
        var dir      = Path.Combine(env.WebRootPath ?? "wwwroot", "group-avatars");
        Directory.CreateDirectory(dir);
        var fileName = $"group_{GetUserId()}_{DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()}{ext}";
        var path     = Path.Combine(dir, fileName);
        using var stream = new FileStream(path, FileMode.Create);
        await file.CopyToAsync(stream);
        var url = $"{Request.Scheme}://{Request.Host}/group-avatars/{fileName}";
        return Ok(new { avatarUrl = url });
    }

    private int GetUserId() =>
        int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
}

public record SendGroupMessageBody(string? Text, string? AttachmentUrl, string? AttachmentType);
