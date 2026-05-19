using FitnessTrainerAPI.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;
using System.Collections.Concurrent;
using System.Security.Claims;

namespace FitnessTrainerAPI.Hubs;

[Authorize]
public class ChatHub(IChatService chatService, IFcmService fcmService) : Hub
{
    private static readonly ConcurrentDictionary<int, HashSet<string>> _online   = new();
    private static readonly ConcurrentDictionary<int, DateTime>        _lastSeen = new();

    public async Task SendMessage(int receiverId, string text)
    {
        var senderId   = GetUserId();
        var senderName = Context.User!.FindFirstValue("firstName") ?? "Пользователь";
        var msg        = await chatService.SendMessageAsync(senderId, new SendMessageRequest(receiverId, text, null, null));

        await Clients.User(receiverId.ToString()).SendAsync("ReceiveMessage", msg);
        await Clients.Caller.SendAsync("MessageSent", msg);

        if (!_online.ContainsKey(receiverId))
            await fcmService.SendMessageNotificationAsync(receiverId, senderName, text);
    }

    public async Task MarkRead(int senderId)
    {
        var receiverId = GetUserId();
        await chatService.MarkAsReadAsync(receiverId, senderId);
        await Clients.User(senderId.ToString()).SendAsync("MessagesRead", receiverId);
    }

    // Клиент запрашивает статус контакта — ответ приходит через событие UserStatusChanged
    public async Task SendTyping(int receiverId, string typingType)
    {
        var senderId = GetUserId();
        await Clients.User(receiverId.ToString()).SendAsync("ReceiveTyping", senderId, typingType);
    }

    public async Task StopTyping(int receiverId)
    {
        var senderId = GetUserId();
        await Clients.User(receiverId.ToString()).SendAsync("ReceiveStopTyping", senderId);
    }

    public async Task GetUserStatus(int targetUserId)
    {
        var isOnline = _online.ContainsKey(targetUserId);
        var lastSeen = _lastSeen.TryGetValue(targetUserId, out var dt) ? dt.ToString("O") : "";
        await Clients.Caller.SendAsync("UserStatusChanged", targetUserId, isOnline, lastSeen);
    }

    public override async Task OnConnectedAsync()
    {
        var userId = GetUserId();
        _online.AddOrUpdate(userId,
            _ => [Context.ConnectionId],
            (_, set) => { lock (set) { set.Add(Context.ConnectionId); } return set; });

        await Groups.AddToGroupAsync(Context.ConnectionId, $"user_{userId}");

        // Уведомить контакты что пользователь онлайн
        await NotifyContactsStatusAsync(userId, isOnline: true, lastSeen: "");

        await base.OnConnectedAsync();
    }

    public override async Task OnDisconnectedAsync(Exception? exception)
    {
        var userId = GetUserId();
        var wentOffline = false;

        if (_online.TryGetValue(userId, out var set))
        {
            lock (set) { set.Remove(Context.ConnectionId); }
            if (set.Count == 0)
            {
                _online.TryRemove(userId, out _);
                wentOffline = true;
            }
        }

        if (wentOffline)
        {
            var lastSeen = DateTime.UtcNow;
            _lastSeen[userId] = lastSeen;
            await NotifyContactsStatusAsync(userId, isOnline: false, lastSeen: lastSeen.ToString("O"));
        }

        await base.OnDisconnectedAsync(exception);
    }

    private async Task NotifyContactsStatusAsync(int userId, bool isOnline, string lastSeen)
    {
        try
        {
            var contacts = await chatService.GetConversationListAsync(userId);
            foreach (var c in contacts)
                await Clients.Group($"user_{c.ContactId}")
                    .SendAsync("UserStatusChanged", userId, isOnline, lastSeen);
        }
        catch { /* не критично */ }
    }

    public static bool IsOnline(int userId) => _online.ContainsKey(userId);

    private int GetUserId() =>
        int.Parse(Context.User!.FindFirstValue(ClaimTypes.NameIdentifier)!);
}
