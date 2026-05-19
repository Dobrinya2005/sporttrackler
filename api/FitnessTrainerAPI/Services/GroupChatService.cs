using FitnessTrainerAPI.Data;
using FitnessTrainerAPI.Models;
using Microsoft.EntityFrameworkCore;

namespace FitnessTrainerAPI.Services;

public record GroupDto(
    int GroupId,
    string Name,
    string? AvatarUrl,
    int CreatedBy,
    List<GroupMemberDto> Members,
    string? LastMessage,
    DateTime? LastMessageAt,
    int UnreadCount
);

public record GroupMemberDto(int UserId, string Name, string? Avatar, bool IsAdmin);

public record GroupMessageDto(
    int MessageId,
    int GroupId,
    int SenderId,
    string SenderName,
    string? SenderAvatar,
    string? MessageText,
    string? AttachmentUrl,
    string? AttachmentType,
    DateTime SentAt
);

public record CreateGroupRequest(string Name, List<int> MemberIds);
public record SendGroupMessageRequest(int GroupId, string? Text, string? AttachmentUrl, string? AttachmentType);

public interface IGroupChatService
{
    Task<GroupDto> CreateGroupAsync(int creatorId, CreateGroupRequest request);
    Task<List<GroupDto>> GetUserGroupsAsync(int userId);
    Task<GroupDto?> GetGroupAsync(int groupId, int userId);
    Task<List<GroupMessageDto>> GetGroupMessagesAsync(int groupId, int userId, int page, int pageSize);
    Task<GroupMessageDto> SendMessageAsync(int senderId, SendGroupMessageRequest request);
    Task AddMemberAsync(int groupId, int requesterId, int newUserId);
    Task RemoveMemberAsync(int groupId, int requesterId, int targetUserId);
    Task<bool> IsMemberAsync(int groupId, int userId);
    Task<List<int>> GetMemberIdsAsync(int groupId);
}

public class GroupChatService(AppDbContext db) : IGroupChatService
{
    public async Task<GroupDto> CreateGroupAsync(int creatorId, CreateGroupRequest request)
    {
        var group = new ChatGroup
        {
            Name      = request.Name.Trim(),
            CreatedBy = creatorId
        };
        db.ChatGroups.Add(group);
        await db.SaveChangesAsync();

        var memberIds = request.MemberIds.Distinct().ToList();
        if (!memberIds.Contains(creatorId)) memberIds.Add(creatorId);

        foreach (var uid in memberIds)
        {
            db.GroupMembers.Add(new GroupMember
            {
                GroupId = group.GroupId,
                UserId  = uid,
                IsAdmin = uid == creatorId
            });
        }
        await db.SaveChangesAsync();

        return await GetGroupAsync(group.GroupId, creatorId) ?? throw new Exception("Group not found");
    }

    public async Task<List<GroupDto>> GetUserGroupsAsync(int userId)
    {
        var groupIds = await db.GroupMembers
            .Where(gm => gm.UserId == userId)
            .Select(gm => gm.GroupId)
            .ToListAsync();

        var result = new List<GroupDto>();
        foreach (var gid in groupIds)
        {
            var dto = await GetGroupAsync(gid, userId);
            if (dto != null) result.Add(dto);
        }
        return result.OrderByDescending(g => g.LastMessageAt ?? DateTime.MinValue).ToList();
    }

    public async Task<GroupDto?> GetGroupAsync(int groupId, int userId)
    {
        var group = await db.ChatGroups
            .Include(g => g.Members).ThenInclude(m => m.User)
            .FirstOrDefaultAsync(g => g.GroupId == groupId);

        if (group == null) return null;

        var lastMsg = await db.GroupMessages
            .Where(m => m.GroupId == groupId && !m.IsDeleted)
            .OrderByDescending(m => m.SentAt)
            .FirstOrDefaultAsync();

        var members = group.Members.Select(m => new GroupMemberDto(
            m.UserId,
            m.User.FirstName + " " + m.User.LastName,
            m.User.AvatarUrl,
            m.IsAdmin
        )).ToList();

        return new GroupDto(
            group.GroupId, group.Name, group.AvatarUrl, group.CreatedBy,
            members,
            lastMsg?.MessageText, lastMsg?.SentAt,
            0
        );
    }

    public async Task<List<GroupMessageDto>> GetGroupMessagesAsync(int groupId, int userId, int page, int pageSize)
    {
        if (!await IsMemberAsync(groupId, userId))
            throw new UnauthorizedAccessException("Not a member");

        return await db.GroupMessages
            .Include(m => m.Sender)
            .Where(m => m.GroupId == groupId && !m.IsDeleted)
            .OrderByDescending(m => m.SentAt)
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .Select(m => new GroupMessageDto(
                m.MessageId, m.GroupId, m.SenderId,
                m.Sender.FirstName + " " + m.Sender.LastName,
                m.Sender.AvatarUrl,
                m.MessageText, m.AttachmentUrl, m.AttachmentType,
                m.SentAt))
            .ToListAsync();
    }

    public async Task<GroupMessageDto> SendMessageAsync(int senderId, SendGroupMessageRequest request)
    {
        if (!await IsMemberAsync(request.GroupId, senderId))
            throw new UnauthorizedAccessException("Not a member");

        var sender = await db.Users.FindAsync(senderId)
            ?? throw new KeyNotFoundException("Sender not found");

        var msg = new GroupMessage
        {
            GroupId        = request.GroupId,
            SenderId       = senderId,
            MessageText    = request.Text,
            AttachmentUrl  = request.AttachmentUrl,
            AttachmentType = request.AttachmentType
        };
        db.GroupMessages.Add(msg);
        await db.SaveChangesAsync();

        return new GroupMessageDto(
            msg.MessageId, msg.GroupId, senderId,
            sender.FirstName + " " + sender.LastName,
            sender.AvatarUrl,
            request.Text, request.AttachmentUrl, request.AttachmentType,
            msg.SentAt);
    }

    public async Task AddMemberAsync(int groupId, int requesterId, int newUserId)
    {
        var requester = await db.GroupMembers
            .FirstOrDefaultAsync(gm => gm.GroupId == groupId && gm.UserId == requesterId)
            ?? throw new UnauthorizedAccessException("Not a member");
        if (!requester.IsAdmin) throw new UnauthorizedAccessException("Not an admin");

        var exists = await db.GroupMembers
            .AnyAsync(gm => gm.GroupId == groupId && gm.UserId == newUserId);
        if (exists) return;

        db.GroupMembers.Add(new GroupMember { GroupId = groupId, UserId = newUserId });
        await db.SaveChangesAsync();
    }

    public async Task RemoveMemberAsync(int groupId, int requesterId, int targetUserId)
    {
        var requester = await db.GroupMembers
            .FirstOrDefaultAsync(gm => gm.GroupId == groupId && gm.UserId == requesterId)
            ?? throw new UnauthorizedAccessException("Not a member");
        if (!requester.IsAdmin && requesterId != targetUserId)
            throw new UnauthorizedAccessException("Not an admin");

        var member = await db.GroupMembers
            .FirstOrDefaultAsync(gm => gm.GroupId == groupId && gm.UserId == targetUserId);
        if (member != null) { db.GroupMembers.Remove(member); await db.SaveChangesAsync(); }
    }

    public async Task<bool> IsMemberAsync(int groupId, int userId) =>
        await db.GroupMembers.AnyAsync(gm => gm.GroupId == groupId && gm.UserId == userId);

    public async Task<List<int>> GetMemberIdsAsync(int groupId) =>
        await db.GroupMembers
            .Where(gm => gm.GroupId == groupId)
            .Select(gm => gm.UserId)
            .ToListAsync();
}
