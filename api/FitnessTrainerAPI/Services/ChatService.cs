using FitnessTrainerAPI.Data;
using FitnessTrainerAPI.Models;
using Microsoft.EntityFrameworkCore;

namespace FitnessTrainerAPI.Services;

public record ReactionDto(string Emoji, int Count, bool MyReaction);

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
    DateTime SentAt,
    int? ReplyToMessageId = null,
    string? ReplyToSenderName = null,
    string? ReplyToText = null,
    List<ReactionDto>? Reactions = null
);

public record SendMessageRequest(int ReceiverId, string? Text, string? AttachmentUrl, string? AttachmentType, int? ReplyToMessageId = null);

public interface IChatService
{
    Task<List<MessageDto>> GetConversationAsync(int userId1, int userId2, int page, int pageSize);
    Task<MessageDto> SendMessageAsync(int senderId, SendMessageRequest request);
    Task MarkAsReadAsync(int receiverId, int senderId);
    Task<List<ConversationPreview>> GetConversationListAsync(int userId);
    Task<List<ReactionDto>> ToggleReactionAsync(int messageId, int userId, string emoji);
    Task<(int SenderId, int ReceiverId)?> GetMessageParticipantsAsync(int messageId);
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
        var messages = await db.Messages
            .Include(m => m.Sender)
            .Include(m => m.ReplyToMessage).ThenInclude(r => r!.Sender)
            .Where(m => !m.IsDeleted &&
                ((m.SenderId == userId1 && m.ReceiverId == userId2) ||
                 (m.SenderId == userId2 && m.ReceiverId == userId1)))
            .OrderByDescending(m => m.SentAt)
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .ToListAsync();

        var messageIds = messages.Select(m => m.MessageId).ToList();
        var allReactions = await db.MessageReactions
            .Where(r => messageIds.Contains(r.MessageId))
            .ToListAsync();

        return messages.Select(m =>
        {
            var reactions = allReactions
                .Where(r => r.MessageId == m.MessageId)
                .GroupBy(r => r.Emoji)
                .Select(g => new ReactionDto(g.Key, g.Count(), g.Any(r => r.UserId == userId1)))
                .ToList();
            return new MessageDto(
                m.MessageId, m.SenderId,
                m.Sender.FirstName + " " + m.Sender.LastName,
                m.Sender.AvatarUrl,
                m.ReceiverId, m.MessageText,
                m.AttachmentUrl, m.AttachmentType,
                m.IsRead, m.SentAt,
                m.ReplyToMessageId,
                m.ReplyToMessage != null ? m.ReplyToMessage.Sender.FirstName + " " + m.ReplyToMessage.Sender.LastName : null,
                m.ReplyToMessage != null ? m.ReplyToMessage.MessageText : null,
                reactions.Count > 0 ? reactions : null);
        }).ToList();
    }

    public async Task<MessageDto> SendMessageAsync(int senderId, SendMessageRequest request)
    {
        var sender = await db.Users.FindAsync(senderId)
            ?? throw new KeyNotFoundException("Sender not found");

        string? replyToSenderName = null;
        string? replyToText = null;
        if (request.ReplyToMessageId.HasValue)
        {
            var replied = await db.Messages.Include(m => m.Sender)
                .FirstOrDefaultAsync(m => m.MessageId == request.ReplyToMessageId.Value);
            if (replied != null)
            {
                replyToSenderName = replied.Sender.FirstName + " " + replied.Sender.LastName;
                replyToText = replied.MessageText;
            }
        }

        var msg = new Message
        {
            SenderId          = senderId,
            ReceiverId        = request.ReceiverId,
            MessageText       = request.Text,
            AttachmentUrl     = request.AttachmentUrl,
            AttachmentType    = request.AttachmentType,
            ReplyToMessageId  = request.ReplyToMessageId
        };
        db.Messages.Add(msg);
        await db.SaveChangesAsync();

        return new MessageDto(
            msg.MessageId, senderId,
            sender.FirstName + " " + sender.LastName,
            sender.AvatarUrl,
            request.ReceiverId, request.Text,
            request.AttachmentUrl, request.AttachmentType,
            false, msg.SentAt,
            request.ReplyToMessageId, replyToSenderName, replyToText);
    }

    public async Task MarkAsReadAsync(int receiverId, int senderId)
    {
        var unread = await db.Messages
            .Where(m => m.ReceiverId == receiverId && m.SenderId == senderId && !m.IsRead)
            .ToListAsync();

        unread.ForEach(m => { m.IsRead = true; m.ReadAt = DateTime.UtcNow; });
        await db.SaveChangesAsync();
    }

    public async Task<List<ReactionDto>> ToggleReactionAsync(int messageId, int userId, string emoji)
    {
        var existing = await db.MessageReactions
            .FirstOrDefaultAsync(r => r.MessageId == messageId && r.UserId == userId && r.Emoji == emoji);

        if (existing != null)
            db.MessageReactions.Remove(existing);
        else
            db.MessageReactions.Add(new MessageReaction { MessageId = messageId, UserId = userId, Emoji = emoji });

        await db.SaveChangesAsync();

        return await db.MessageReactions
            .Where(r => r.MessageId == messageId)
            .GroupBy(r => r.Emoji)
            .Select(g => new ReactionDto(g.Key, g.Count(), g.Any(r => r.UserId == userId)))
            .ToListAsync();
    }

    public async Task<(int SenderId, int ReceiverId)?> GetMessageParticipantsAsync(int messageId)
    {
        var msg = await db.Messages.FindAsync(messageId);
        return msg is null ? null : (msg.SenderId, msg.ReceiverId);
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
