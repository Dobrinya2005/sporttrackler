using FitnessTrainerAPI.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System.Security.Claims;

namespace FitnessTrainerAPI.Controllers;

[ApiController]
[Route("api/messages")]
[Authorize]
public class ChatController(IChatService chatService) : ControllerBase
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
        var result = await chatService.SendMessageAsync(GetUserId(), request);
        return Ok(result);
    }

    [HttpPost("{senderId:int}/read")]
    public async Task<IActionResult> MarkRead(int senderId)
    {
        await chatService.MarkAsReadAsync(GetUserId(), senderId);
        return NoContent();
    }

    private int GetUserId() =>
        int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
}
