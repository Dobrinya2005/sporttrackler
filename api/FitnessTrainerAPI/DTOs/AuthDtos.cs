namespace FitnessTrainerAPI.DTOs;

public class RegisterRequest
{
    public string FirstName { get; set; } = string.Empty;
    public string LastName  { get; set; } = string.Empty;
    public string Email     { get; set; } = string.Empty;
    public string Password  { get; set; } = string.Empty;
    public string Role      { get; set; } = string.Empty;
    public string? Phone    { get; set; }
}

public class LoginRequest
{
    public string Email    { get; set; } = string.Empty;
    public string Password { get; set; } = string.Empty;
}

public record AuthResponse(
    int UserId,
    string FirstName,
    string LastName,
    string Email,
    string Role,
    string? AvatarUrl,
    string AccessToken,
    string RefreshToken
);

public record RefreshTokenRequest(string RefreshToken);

public record TokenResponse(string AccessToken, string RefreshToken);
