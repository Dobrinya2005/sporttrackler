using System.ComponentModel.DataAnnotations;

namespace FitnessTrainerAPI.Models;

public class Message
{
    public int MessageId { get; set; }
    public int SenderId { get; set; }
    public int ReceiverId { get; set; }
    public string? MessageText { get; set; }
    public string? AttachmentUrl { get; set; }
    public string? AttachmentType { get; set; }
    public bool IsRead { get; set; }
    public DateTime SentAt { get; set; } = DateTime.UtcNow;
    public DateTime? ReadAt { get; set; }
    public bool IsDeleted { get; set; }

    public User Sender { get; set; } = null!;
    public User Receiver { get; set; } = null!;
}

public class Notification
{
    public int NotificationId { get; set; }
    public int UserId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string? Body { get; set; }
    public string? NotifType { get; set; }
    public int? EntityId { get; set; }
    public bool IsRead { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public User User { get; set; } = null!;
}

public class FcmToken
{
    [Key]
    public int TokenId { get; set; }
    public int UserId { get; set; }
    public string DeviceToken { get; set; } = string.Empty;
    public string DevicePlatform { get; set; } = "android";
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    public User User { get; set; } = null!;
}
