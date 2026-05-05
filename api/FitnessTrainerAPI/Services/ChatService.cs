using FitnessTrainerAPI.Data;
using FitnessTrainerAPI.Models;
using Microsoft.EntityFrameworkCore;

namespace FitnessTrainerAPI.Services;

public record MessageDto(
    int MessageId,
    int SenderId,
    string SenderName,
    string? SenderAvatar,
    int ReceiverId,
    string? MessageText,
    string? AttachmentUrl,
    string? AttachmentType,
    bool IsRead,
    DateTime SentAt
);

public record SendMessageRequest(int ReceiverId, string? Text, string? AttachmentUrl, string? AttachmentType);

public interface IChatService
{
    Task<List<MessageDto>> GetConversationAsync(int userId1, int userId2, int page, int pageSize);
    Task<MessageDto> SendMessageAsync(int senderId, SendMessageRequest request);
    Task MarkAsReadAsync(int receiverId, int senderId);
    Task<List<ConversationPreview>> GetConversationListAsync(int userId);
}

public record ConversationPreview(
    int ContactId,
    string ContactName,
    string? ContactAvatar,
    string? LastMessage,
    DateTime LastMessageAt,
    int UnreadCount
);

public class ChatService(AppDbContext db) : IChatService
{
    public async Task<List<MessageDto>> GetConversationAsync(int userId1, int userId2, int page, int pageSize)
    {
        return await db.Messages
            .Include(m => m.Sender)
            .Where(m => !m.IsDeleted &&
                ((m.SenderId == userId1 && m.ReceiverId == userId2) ||
                 (m.SenderId == userId2 && m.ReceiverId == userId1)))
            .OrderByDescending(m => m.SentAt)
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .Select(m => new MessageDto(
                m.MessageId, m.SenderId,
                m.Sender.FirstName + " " + m.Sender.LastName,
                m.Sender.AvatarUrl,
                m.ReceiverId, m.MessageText,
                m.AttachmentUrl, m.AttachmentType,
                m.IsRead, m.SentAt))
            .ToListAsync();
    }

    public async Task<MessageDto> SendMessageAsync(int senderId, SendMessageRequest request)
    {
        var sender = await db.Users.FindAsync(senderId)
            ?? throw new KeyNotFoundException("Sender not found");

        var msg = new Message
        {
            SenderId       = senderId,
            ReceiverId     = request.ReceiverId,
            MessageText    = request.Text,
            AttachmentUrl  = request.AttachmentUrl,
            AttachmentType = request.AttachmentType
        };
        db.Messages.Add(msg);
        await db.SaveChangesAsync();

        return new MessageDto(
            msg.MessageId, senderId,
            sender.FirstName + " " + sender.LastName,
            sender.AvatarUrl,
            request.ReceiverId, request.Text,
            request.AttachmentUrl, request.AttachmentType,
            false, msg.SentAt);
    }

    public async Task MarkAsReadAsync(int receiverId, int senderId)
    {
        var unread = await db.Messages
            .Where(m => m.ReceiverId == receiverId && m.SenderId == senderId && !m.IsRead)
            .ToListAsync();

        unread.ForEach(m => { m.IsRead = true; m.ReadAt = DateTime.UtcNow; });
        await db.SaveChangesAsync();
    }

    public async Task<List<ConversationPreview>> GetConversationListAsync(int userId)
    {
        var contacts = await db.Messages
            .Where(m => !m.IsDeleted && (m.SenderId == userId || m.ReceiverId == userId))
            .GroupBy(m => m.SenderId == userId ? m.ReceiverId : m.SenderId)
            .Select(g => new
            {
                ContactId       = g.Key,
                LastMessage     = g.OrderByDescending(m => m.SentAt).First(),
                UnreadCount     = g.Count(m => m.ReceiverId == userId && !m.IsRead)
            })
            .ToListAsync();

        var result = new List<ConversationPreview>();
        foreach (var c in contacts)
        {
            var contact = await db.Users.FindAsync(c.ContactId);
            if (contact is null) continue;
            result.Add(new ConversationPreview(
                c.ContactId,
                contact.FirstName + " " + contact.LastName,
                contact.AvatarUrl,
                c.LastMessage.MessageText,
                c.LastMessage.SentAt,
                c.UnreadCount));
        }
        return result.OrderByDescending(c => c.LastMessageAt).ToList();
    }
}
