using System.IdentityModel.Tokens.Jwt;
using System.Linq;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using FitnessTrainerAPI.Data;
using FitnessTrainerAPI.DTOs;
using FitnessTrainerAPI.Models;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;

namespace FitnessTrainerAPI.Services;

public interface IAuthService
{
    Task<AuthResponse?> RegisterAsync(RegisterRequest request);
    Task<AuthResponse?> LoginAsync(LoginRequest request);
    Task<TokenResponse?> RefreshTokenAsync(string refreshToken);
    Task RevokeTokenAsync(int userId);
}

public class AuthService(AppDbContext db, IConfiguration config) : IAuthService
{
    public async Task<AuthResponse?> RegisterAsync(RegisterRequest request)
    {
        if (await db.Users.AnyAsync(u => u.Email == request.Email))
            return null;

        var role = await db.Roles.FirstOrDefaultAsync(r => r.RoleName == request.Role);
        if (role is null) return null;

        var user = new User
        {
            RoleId       = role.RoleId,
            FirstName    = request.FirstName,
            LastName     = request.LastName,
            Email        = request.Email,
            PasswordHash = BCrypt.Net.BCrypt.HashPassword(request.Password),
            Phone        = request.Phone
        };

        db.Users.Add(user);
        await db.SaveChangesAsync();

        if (role.RoleName == "Trainer")
            db.TrainerProfiles.Add(new TrainerProfile { UserId = user.UserId, TrainerCode = GenerateTrainerCode() });
        else
            db.ClientProfiles.Add(new ClientProfile { UserId = user.UserId });

        await db.SaveChangesAsync();

        var tokens = await CreateTokensAsync(user, role.RoleName);
        return new AuthResponse(user.UserId, user.FirstName, user.LastName,
            user.Email, role.RoleName, user.AvatarUrl,
            tokens.AccessToken, tokens.RefreshToken);
    }

    public async Task<AuthResponse?> LoginAsync(LoginRequest request)
    {
        var user = await db.Users
            .Include(u => u.Role)
            .FirstOrDefaultAsync(u => u.Email == request.Email && u.IsActive);

        if (user is null || !BCrypt.Net.BCrypt.Verify(request.Password, user.PasswordHash))
            return null;

        var tokens = await CreateTokensAsync(user, user.Role.RoleName);
        return new AuthResponse(user.UserId, user.FirstName, user.LastName,
            user.Email, user.Role.RoleName, user.AvatarUrl,
            tokens.AccessToken, tokens.RefreshToken);
    }

    public async Task<TokenResponse?> RefreshTokenAsync(string refreshToken)
    {
        var token = await db.RefreshTokens
            .Include(t => t.User).ThenInclude(u => u.Role)
            .FirstOrDefaultAsync(t => t.Token == refreshToken && !t.IsRevoked);

        if (token is null || token.ExpiresAt < DateTime.UtcNow) return null;

        token.IsRevoked = true;
        var tokens = await CreateTokensAsync(token.User, token.User.Role.RoleName);
        await db.SaveChangesAsync();
        return tokens;
    }

    public async Task RevokeTokenAsync(int userId)
    {
        var tokens = await db.RefreshTokens
            .Where(t => t.UserId == userId && !t.IsRevoked)
            .ToListAsync();
        tokens.ForEach(t => t.IsRevoked = true);
        await db.SaveChangesAsync();
    }

    private async Task<TokenResponse> CreateTokensAsync(User user, string roleName)
    {
        var accessToken  = GenerateAccessToken(user, roleName);
        var refreshToken = GenerateRefreshToken();

        db.RefreshTokens.Add(new RefreshToken
        {
            UserId    = user.UserId,
            Token     = refreshToken,
            ExpiresAt = DateTime.UtcNow.AddDays(
                config.GetValue<int>("JwtSettings:RefreshTokenExpiryDays"))
        });
        await db.SaveChangesAsync();

        return new TokenResponse(accessToken, refreshToken);
    }

    private string GenerateAccessToken(User user, string roleName)
    {
        var key    = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(config["JwtSettings:SecretKey"]!));
        var creds  = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);
        var expiry = DateTime.UtcNow.AddMinutes(config.GetValue<int>("JwtSettings:AccessTokenExpiryMinutes"));

        var claims = new[]
        {
            new Claim(ClaimTypes.NameIdentifier, user.UserId.ToString()),
            new Claim(ClaimTypes.Email,          user.Email),
            new Claim(ClaimTypes.Role,           roleName),
            new Claim("firstName",               user.FirstName)
        };

        var token = new JwtSecurityToken(
            issuer:             config["JwtSettings:Issuer"],
            audience:           config["JwtSettings:Audience"],
            claims:             claims,
            expires:            expiry,
            signingCredentials: creds);

        return new JwtSecurityTokenHandler().WriteToken(token);
    }

    private static string GenerateTrainerCode()
    {
        const string chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        var bytes = new byte[6];
        RandomNumberGenerator.Fill(bytes);
        return new string(bytes.Select(b => chars[b % chars.Length]).ToArray());
    }

    private static string GenerateRefreshToken()
    {
        var bytes = new byte[64];
        RandomNumberGenerator.Fill(bytes);
        return Convert.ToBase64String(bytes);
    }
}
