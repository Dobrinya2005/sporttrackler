using FitnessTrainerAPI.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;
using System.Collections.Concurrent;
using System.Security.Claims;

namespace FitnessTrainerAPI.Hubs;

[Authorize]
public class ChatHub(IChatService chatService, IFcmService fcmService) : Hub
{
    // userId → set of connectionIds (для определения онлайн-статуса)
    private static readonly ConcurrentDictionary<int, HashSet<string>> _online = new();

    public async Task SendMessage(int receiverId, string text)
    {
        var senderId   = GetUserId();
        var senderName = Context.User!.FindFirstValue("firstName") ?? "Пользователь";
        var msg        = await chatService.SendMessageAsync(senderId, new SendMessageRequest(receiverId, text, null, null));

        await Clients.User(receiverId.ToString()).SendAsync("ReceiveMessage", msg);
        await Clients.Caller.SendAsync("MessageSent", msg);

        // Отправить FCM если получатель офлайн
        if (!_online.ContainsKey(receiverId))
            await fcmService.SendMessageNotificationAsync(receiverId, senderName, text);
    }

    public async Task MarkRead(int senderId)
    {
        var receiverId = GetUserId();
        await chatService.MarkAsReadAsync(receiverId, senderId);
        await Clients.User(senderId.ToString()).SendAsync("MessagesRead", receiverId);
    }

    public override async Task OnConnectedAsync()
    {
        var userId = GetUserId();
        _online.AddOrUpdate(userId,
            _ => [Context.ConnectionId],
            (_, set) => { lock (set) { set.Add(Context.ConnectionId); } return set; });

        await Groups.AddToGroupAsync(Context.ConnectionId, $"user_{userId}");
        await base.OnConnectedAsync();
    }

    public override async Task OnDisconnectedAsync(Exception? exception)
    {
        var userId = GetUserId();
        if (_online.TryGetValue(userId, out var set))
        {
            lock (set) { set.Remove(Context.ConnectionId); }
            if (set.Count == 0) _online.TryRemove(userId, out _);
        }
        await base.OnDisconnectedAsync(exception);
    }

    private int GetUserId() =>
        int.Parse(Context.User!.FindFirstValue(ClaimTypes.NameIdentifier)!);
}
