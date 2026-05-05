using FitnessTrainerAPI.Data;
using FirebaseAdmin;
using FirebaseAdmin.Messaging;
using Google.Apis.Auth.OAuth2;
using Microsoft.EntityFrameworkCore;

namespace FitnessTrainerAPI.Services;

public interface IFcmService
{
    Task RegisterTokenAsync(int userId, string deviceToken);
    Task SendMessageNotificationAsync(int receiverId, string senderName, string messageText);
}

public class FcmService(AppDbContext db, IConfiguration config, ILogger<FcmService> logger) : IFcmService
{
    private static bool _initialized;

    private void EnsureInitialized()
    {
        if (_initialized) return;
        var credPath = config["Firebase:CredentialPath"];
        if (string.IsNullOrEmpty(credPath) || !File.Exists(credPath)) return;

        if (FirebaseApp.DefaultInstance is null)
        {
            FirebaseApp.Create(new AppOptions
            {
                Credential = GoogleCredential.FromFile(credPath)
            });
        }
        _initialized = true;
    }

    public async Task RegisterTokenAsync(int userId, string deviceToken)
    {
        var existing = await db.FcmTokens
            .FirstOrDefaultAsync(t => t.UserId == userId && t.DeviceToken == deviceToken);

        if (existing is null)
        {
            db.FcmTokens.Add(new Models.FcmToken
            {
                UserId        = userId,
                DeviceToken   = deviceToken,
                DevicePlatform = "android"
            });
        }
        else
        {
            existing.UpdatedAt = DateTime.UtcNow;
        }
        await db.SaveChangesAsync();
    }

    public async Task SendMessageNotificationAsync(int receiverId, string senderName, string messageText)
    {
        EnsureInitialized();
        if (!_initialized) return;

        var tokens = await db.FcmTokens
            .Where(t => t.UserId == receiverId)
            .Select(t => t.DeviceToken)
            .ToListAsync();

        if (tokens.Count == 0) return;

        var preview = messageText.Length > 80 ? messageText[..80] + "…" : messageText;

        foreach (var token in tokens)
        {
            try
            {
                await FirebaseMessaging.DefaultInstance.SendAsync(new Message
                {
                    Token = token,
                    Notification = new Notification
                    {
                        Title = senderName,
                        Body  = preview
                    },
                    Android = new AndroidConfig
                    {
                        Priority = Priority.High,
                        Notification = new AndroidNotification
                        {
                            ChannelId = "chat_messages",
                            Sound     = "default"
                        }
                    }
                });
            }
            catch (Exception ex)
            {
                logger.LogWarning("FCM send failed for token {Token}: {Error}", token, ex.Message);
            }
        }
    }
}
