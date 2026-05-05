using FitnessTrainerAPI.DTOs;
using FitnessTrainerAPI.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System.Security.Claims;

namespace FitnessTrainerAPI.Controllers;

[ApiController]
[Route("api/food")]
[Authorize]
public class FoodController(IFoodService foodService) : ControllerBase
{
    [HttpGet("search")]
    public async Task<IActionResult> Search([FromQuery] string q)
    {
        if (string.IsNullOrWhiteSpace(q)) return BadRequest();
        return Ok(await foodService.SearchProductsAsync(q));
    }

    [HttpPost("products")]
    public async Task<IActionResult> CreateProduct([FromBody] FoodProductCreateRequest request)
    {
        var result = await foodService.CreateCustomProductAsync(GetUserId(), request);
        return Ok(result);
    }

    [HttpPost("diary")]
    public async Task<IActionResult> AddEntry([FromBody] DiaryEntryCreateRequest request)
    {
        var result = await foodService.AddDiaryEntryAsync(GetUserId(), request);
        return Ok(result);
    }

    [HttpGet("diary/{clientId:int}/{date}")]
    public async Task<IActionResult> GetDailySummary(int clientId, DateOnly date)
    {
        if (!CanAccessClient(clientId)) return Forbid();
        return Ok(await foodService.GetDailySummaryAsync(clientId, date));
    }

    [HttpDelete("diary/{diaryId:int}")]
    public async Task<IActionResult> DeleteEntry(int diaryId)
    {
        var deleted = await foodService.DeleteDiaryEntryAsync(diaryId, GetUserId());
        if (!deleted) return NotFound();
        return NoContent();
    }

    private int GetUserId() =>
        int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);

    private bool CanAccessClient(int clientId) =>
        GetUserId() == clientId || User.IsInRole("Trainer");
}
