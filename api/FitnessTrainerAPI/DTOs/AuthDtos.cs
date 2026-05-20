using System.ComponentModel.DataAnnotations;

namespace FitnessTrainerAPI.DTOs;

public class RegisterRequest
{
    public string FirstName     { get; set; } = string.Empty;
    public string LastName      { get; set; } = string.Empty;
    public string Email         { get; set; } = string.Empty;
    [StrongPassword]
    public string Password      { get; set; } = string.Empty;
    public string Role          { get; set; } = string.Empty;
    public string? Phone        { get; set; }
    public string? TrainerCode  { get; set; }
}

public class StrongPasswordAttribute : ValidationAttribute
{
    protected override ValidationResult? IsValid(object? value, ValidationContext ctx)
    {
        var pwd = value as string ?? "";
        if (pwd.Length < 8)
            return new ValidationResult("Пароль должен содержать минимум 8 символов");
        if (!pwd.Any(char.IsUpper))
            return new ValidationResult("Пароль должен содержать хотя бы одну заглавную букву");
        if (!pwd.Any(char.IsLower))
            return new ValidationResult("Пароль должен содержать хотя бы одну строчную букву");
        if (!pwd.Any(c => !char.IsLetterOrDigit(c)))
            return new ValidationResult("Пароль должен содержать хотя бы один специальный символ");
        return ValidationResult.Success;
    }
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

public record SendCodeRequest(string Email);
public record VerifyCodeRequest(string Email, string Code);
public record ResetPasswordRequest(string Email, string Code, [StrongPassword] string NewPassword);
