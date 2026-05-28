package com.fitnesstrainer.app.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnesstrainer.app.App
import kotlinx.coroutines.launch

sealed class ChatListItem {
    data class PersonItem(
        val userId: Int,
        val name: String,
        val avatar: String?,
        val lastMessage: String? = null,
        val lastMessageAt: String? = null,
        val unreadCount: Int = 0
    ) : ChatListItem()
    data class GroupItem(
        val groupId: Int,
        val name: String,
        val avatar: String?,
        val lastMessage: String?,
        val lastMessageAt: String? = null,
        val unreadCount: Int = 0
    ) : ChatListItem()
}

class ChatsViewModel : ViewModel() {

    private val _items = MutableLiveData<List<ChatListItem>>()
    val items: LiveData<List<ChatListItem>> = _items

    fun load() {
        viewModelScope.launch {
            try {
                val storage = App.instance.tokenStorage
                val role = storage.getUserRole()
                val api = App.instance.apiService
                val list = mutableListOf<ChatListItem>()

                // Fetch conversation previews (last message, unread count)
                val convMap = try {
                    val resp = api.getConversations()
                    if (resp.isSuccessful) resp.body()?.associateBy { it.contactId } else emptyMap()
                } catch (_: Exception) { emptyMap() } ?: emptyMap()

                if (role == "Client") {
                    // Show all conversations (current + previous trainers)
                    convMap.values.forEach { conv ->
                        list.add(ChatListItem.PersonItem(
                            userId        = conv.contactId,
                            name          = conv.contactName,
                            avatar        = conv.contactAvatar,
                            lastMessage   = conv.lastMessage,
                            lastMessageAt = conv.lastMessageAt,
                            unreadCount   = conv.unreadCount
                        ))
                    }
                } else if (role == "Trainer") {
                    try {
                        val resp = api.getMyClients()
                        if (resp.isSuccessful) {
                            resp.body()?.forEach { c ->
                                val conv = convMap[c.userId]
                                list.add(ChatListItem.PersonItem(
                                    userId       = c.userId,
                                    name         = "${c.firstName} ${c.lastName}",
                                    avatar       = c.avatarUrl,
                                    lastMessage  = conv?.lastMessage,
                                    lastMessageAt = conv?.lastMessageAt,
                                    unreadCount  = conv?.unreadCount ?: 0
                                ))
                            }
                        }
                    } catch (_: Exception) {}
                }

                try {
                    val resp = api.getMyGroups()
                    if (resp.isSuccessful) {
                        resp.body()?.forEach { g ->
                            list.add(ChatListItem.GroupItem(
                                groupId      = g.groupId,
                                name         = g.name,
                                avatar       = g.avatarUrl,
                                lastMessage  = g.lastMessage,
                                lastMessageAt = g.lastMessageAt,
                                unreadCount  = g.unreadCount
                            ))
                        }
                    }
                } catch (_: Exception) {}

                // Sort by last message time (most recent first)
                val sorted = list.sortedByDescending { item ->
                    when (item) {
                        is ChatListItem.PersonItem -> item.lastMessageAt ?: ""
                        is ChatListItem.GroupItem  -> item.lastMessageAt ?: ""
                    }
                }
                _items.postValue(sorted)
            } catch (_: Exception) {
                _items.postValue(emptyList())
            }
        }
    }
}
