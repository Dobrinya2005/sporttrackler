package com.fitnesstrainer.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.BuildConfig
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.model.MessageDto
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatInfoBottomSheet(
    private val contactId: Int,
    private val contactName: String,
    private val messages: List<MessageDto>,
    private val onThemeSelected: (ChatThemeManager.Theme) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_chat_info, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Avatar initials
        val initials = contactName.trim().split(" ")
            .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
        view.findViewById<TextView>(R.id.tv_info_initials).text = initials

        // Load avatar
        val ivAvatar = view.findViewById<ImageView>(R.id.iv_info_avatar)
        val tvInitials = view.findViewById<TextView>(R.id.tv_info_initials)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = App.instance.apiService.getConversations()
                val avatarRaw = resp.body()?.find { it.contactId == contactId }?.contactAvatar
                val avatarUrl = avatarRaw?.let {
                    if (it.startsWith("http")) it
                    else BuildConfig.BASE_URL.trimEnd('/') + it
                }
                withContext(Dispatchers.Main) {
                    if (!avatarUrl.isNullOrBlank() && isAdded) {
                        ivAvatar.visibility = View.VISIBLE
                        tvInitials.visibility = View.GONE
                        Glide.with(requireContext())
                            .load(avatarUrl)
                            .transform(CircleCrop())
                            .into(ivAvatar)
                    }
                }
            } catch (_: Exception) {}
        }

        // Name + message count
        view.findViewById<TextView>(R.id.tv_info_name).text = contactName
        view.findViewById<TextView>(R.id.tv_info_stats).text =
            "${messages.size} сообщений"

        // Media counts
        val urlRegex = Regex("https?://\\S+")
        val photoCount = messages.count { it.attachmentType == "image" }
        val videoCount = messages.count { it.attachmentType == "video" }
        val voiceCount = messages.count { it.attachmentType == "audio" || it.attachmentType == "voice" }
        val linkCount  = messages.count { it.messageText?.let { t -> urlRegex.containsMatchIn(t) } == true }

        view.findViewById<TextView>(R.id.tv_count_photos).text = photoCount.toString()
        view.findViewById<TextView>(R.id.tv_count_videos).text = videoCount.toString()
        view.findViewById<TextView>(R.id.tv_count_voice).text  = voiceCount.toString()
        view.findViewById<TextView>(R.id.tv_count_links).text  = linkCount.toString()

        // Theme picker
        val current = ChatThemeManager.load(requireContext(), contactId)
        val themeMap = mapOf(
            R.id.theme_default to ChatThemeManager.Theme.DEFAULT,
            R.id.theme_navy    to ChatThemeManager.Theme.NAVY,
            R.id.theme_forest  to ChatThemeManager.Theme.FOREST,
            R.id.theme_sunset  to ChatThemeManager.Theme.SUNSET,
            R.id.theme_galaxy  to ChatThemeManager.Theme.GALAXY,
            R.id.theme_ocean   to ChatThemeManager.Theme.OCEAN,
        )
        val checkMap = mapOf(
            R.id.theme_default to R.id.check_default,
            R.id.theme_navy    to R.id.check_navy,
            R.id.theme_forest  to R.id.check_forest,
            R.id.theme_sunset  to R.id.check_sunset,
            R.id.theme_galaxy  to R.id.check_galaxy,
            R.id.theme_ocean   to R.id.check_ocean,
        )

        fun updateChecks(selected: ChatThemeManager.Theme) {
            themeMap.forEach { (frameId, theme) ->
                val checkId = checkMap[frameId] ?: return@forEach
                view.findViewById<ImageView>(checkId).visibility =
                    if (theme == selected) View.VISIBLE else View.GONE
            }
        }

        updateChecks(current)

        themeMap.forEach { (frameId, theme) ->
            view.findViewById<View>(frameId).setOnClickListener {
                ChatThemeManager.save(requireContext(), contactId, theme)
                updateChecks(theme)
                onThemeSelected(theme)
            }
        }
    }
}
