using FitnessTrainerAPI.DTOs;
using FitnessTrainerAPI.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System.Security.Claims;

namespace FitnessTrainerAPI.Controllers;

[ApiController]
[Route("api/measurements")]
[Authorize]
public class MeasurementsController(IMeasurementService service) : ControllerBase
{
    [HttpPost]
    public async Task<IActionResult> Add([FromBody] MeasurementCreateRequest request)
    {
        var clientId = GetUserId();
        var result = await service.AddAsync(clientId, request);
        return CreatedAtAction(nameof(GetLatest), new { clientId }, result);
    }

    [HttpGet("{clientId:int}")]
    public async Task<IActionResult> GetHistory(
        int clientId,
        [FromQuery] DateOnly? from,
        [FromQuery] DateOnly? to)
    {
        if (!CanAccessClient(clientId)) return Forbid();
        var result = await service.GetHistoryAsync(clientId, from, to);
        return Ok(result);
    }

    [HttpGet("{clientId:int}/latest")]
    public async Task<IActionResult> GetLatest(int clientId)
    {
        if (!CanAccessClient(clientId)) return Forbid();
        var result = await service.GetLatestAsync(clientId);
        if (result is null) return NotFound();
        return Ok(result);
    }

    [HttpDelete("{measurementId:int}")]
    public async Task<IActionResult> Delete(int measurementId)
    {
        var deleted = await service.DeleteAsync(measurementId, GetUserId());
        if (!deleted) return NotFound();
        return NoContent();
    }

    private int GetUserId() =>
        int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);

    // Клиент может смотреть только свои данные, тренер — всех своих клиентов
    private bool CanAccessClient(int clientId) =>
        GetUserId() == clientId || User.IsInRole("Trainer");
}
