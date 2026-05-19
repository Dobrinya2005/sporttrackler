package com.fitnesstrainer.app.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnesstrainer.app.App
import kotlinx.coroutines.launch

sealed class ChatListItem {
    data class PersonItem(val userId: Int, val name: String, val avatar: String?) : ChatListItem()
    data class GroupItem(val groupId: Int, val name: String, val avatar: String?, val lastMessage: String?) : ChatListItem()
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

                if (role == "Client") {
                    try {
                        val resp = api.getMyTrainer()
                        if (resp.isSuccessful) {
                            resp.body()?.let { t ->
                                list.add(ChatListItem.PersonItem(
                                    userId = t.userId,
                                    name = "${t.firstName} ${t.lastName}",
                                    avatar = t.avatarUrl
                                ))
                            }
                        }
                    } catch (_: Exception) {}
                } else if (role == "Trainer") {
                    try {
                        val resp = api.getMyClients()
                        if (resp.isSuccessful) {
                            resp.body()?.forEach { c ->
                                list.add(ChatListItem.PersonItem(
                                    userId = c.userId,
                                    name = "${c.firstName} ${c.lastName}",
                                    avatar = c.avatarUrl
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
                                groupId = g.groupId,
                                name = g.name,
                                avatar = g.avatarUrl,
                                lastMessage = g.lastMessage
                            ))
                        }
                    }
                } catch (_: Exception) {}

                _items.postValue(list)
            } catch (_: Exception) {
                _items.postValue(emptyList())
            }
        }
    }
}
