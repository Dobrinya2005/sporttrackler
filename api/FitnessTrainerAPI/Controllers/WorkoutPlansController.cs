using FitnessTrainerAPI.DTOs;
using FitnessTrainerAPI.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System.Security.Claims;

namespace FitnessTrainerAPI.Controllers;

[ApiController]
[Route("api/plans")]
[Authorize]
public class WorkoutPlansController(IWorkoutService service) : ControllerBase
{
    // GET api/plans/{clientId}
    [HttpGet("{clientId:int}")]
    public async Task<IActionResult> GetPlans(int clientId)
    {
        if (!CanAccessClient(clientId)) return Forbid();
        var result = await service.GetPlansAsync(clientId);
        return Ok(result);
    }

    // POST api/plans
    [HttpPost]
    [Authorize(Roles = "Trainer")]
    public async Task<IActionResult> CreatePlan([FromBody] WorkoutPlanCreateRequest request)
    {
        var trainerId = GetUserId();
        var result = await service.CreatePlanAsync(trainerId, request);
        return CreatedAtAction(nameof(GetPlans), new { clientId = request.ClientId }, result);
    }

    // DELETE api/plans/{planId}
    [HttpDelete("{planId:int}")]
    [Authorize(Roles = "Trainer")]
    public async Task<IActionResult> DeletePlan(int planId)
    {
        var deleted = await service.DeletePlanAsync(planId, GetUserId());
        if (!deleted) return NotFound();
        return NoContent();
    }

    private int GetUserId() =>
        int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);

    private bool CanAccessClient(int clientId) =>
        GetUserId() == clientId || User.IsInRole("Trainer");
}
