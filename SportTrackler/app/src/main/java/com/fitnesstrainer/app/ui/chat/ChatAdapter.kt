package com.fitnesstrainer.app.ui.chat

import android.app.Dialog
import android.graphics.Color
import android.graphics.Outline
import android.graphics.SurfaceTexture
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.VideoView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import android.graphics.drawable.GradientDrawable
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.model.MessageDto
import com.fitnesstrainer.app.databinding.ItemMessageReceivedBinding
import com.fitnesstrainer.app.databinding.ItemMessageSentBinding
import java.io.File
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val TYPE_SENT     = 0
private const val TYPE_RECEIVED = 1

class ChatAdapter(
    private val myUserId: Int,
    private val accessToken: String? = null,
    var onReply: ((MessageDto) -> Unit)? = null
) : ListAdapter<MessageDto, RecyclerView.ViewHolder>(DIFF) {

    var searchQuery: String = ""
    var onReact: ((MessageDto, String) -> Unit)? = null
    var theme: ChatThemeManager.Theme = ChatThemeManager.Theme.DEFAULT

    private fun showEmojiPickerForMessage(anchor: android.view.View, msg: MessageDto) {
        val ctx = anchor.context
        val emojis = listOf("👍","❤️","😂","😮","😢","🔥","👏","🎉","🥗","🗿","💪")
        val density = ctx.resources.displayMetrics.density
        val dp4  = (4  * density).toInt()
        val dp8  = (8  * density).toInt()
        val dp12 = (12 * density).toInt()
        val dp44 = (44 * density).toInt()
        val dp16 = (16 * density).toInt()
        val screenW = ctx.resources.displayMetrics.widthPixels

        // ── Popup content ──────────────────────────────────────
        val root = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 20 * density
                setColor(Color.parseColor("#1E2235"))
            }
            elevation = 24 * density
        }

        // Emoji row (horizontal scroll)
        val scroll = android.widget.HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(dp8, dp8, dp8, dp4)
        }
        val emojiRow = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
        }
        var popup: android.widget.PopupWindow? = null
        emojis.forEach { emoji ->
            emojiRow.addView(android.widget.TextView(ctx).apply {
                text = emoji
                textSize = 28f
                gravity = android.view.Gravity.CENTER
                layoutParams = android.widget.LinearLayout.LayoutParams(dp44, dp44).also {
                    it.marginEnd = dp4
                }
                setOnClickListener {
                    onReact?.invoke(msg, emoji)
                    popup?.dismiss()
                }
            })
        }
        scroll.addView(emojiRow)
        root.addView(scroll)

        // Divider
        root.addView(android.view.View(ctx).apply {
            setBackgroundColor(Color.parseColor("#2A2D3E"))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1)
        })

        // Reply button
        root.addView(android.widget.TextView(ctx).apply {
            text = "↩  Ответить"
            textSize = 14f
            setTextColor(Color.parseColor("#CCCCCC"))
            setPadding(dp16, dp12, dp16, dp12)
            setOnClickListener { onReply?.invoke(msg); popup?.dismiss() }
        })

        // ── Measure & position ─────────────────────────────────
        root.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(screenW - dp8 * 4, android.view.View.MeasureSpec.AT_MOST),
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
        )
        val popW = root.measuredWidth
        val popH = root.measuredHeight

        val loc = IntArray(2)
        anchor.getLocationOnScreen(loc)
        val anchorX = loc[0]
        val anchorY = loc[1]

        // x: center above message, clamp to screen
        var xOff = (anchor.width - popW) / 2
        val absX = (anchorX + xOff).coerceIn(dp8, screenW - popW - dp8)
        xOff = absX - anchorX

        // y: above the anchor
        val yOff = -(popH + anchor.height)

        popup = android.widget.PopupWindow(root, android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            isOutsideTouchable = true
            isFocusable = true
        }

        // Animate: scale from bottom-center
        root.pivotX = popW / 2f
        root.pivotY = popH.toFloat()
        root.scaleX = 0.5f
        root.scaleY = 0.5f
        root.alpha = 0f

        popup.showAsDropDown(anchor, xOff, yOff)

        root.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(180)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
            .start()
    }

    override fun getItemViewType(position: Int) =
        if (getItem(position).senderId == myUserId) TYPE_SENT else TYPE_RECEIVED

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_SENT)
            SentVH(ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false), accessToken)
        else
            ReceivedVH(ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false), accessToken)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg  = getItem(position)
        val time = formatTime(msg.sentAt)
        when (holder) {
            is SentVH     -> holder.bind(msg, time, searchQuery, onReact, theme)
            is ReceivedVH -> holder.bind(msg, time, searchQuery, onReact, theme)
        }
        holder.itemView.setOnLongClickListener {
            showEmojiPickerForMessage(holder.itemView, msg)
            true
        }
    }

    class SentVH(private val b: ItemMessageSentBinding, private val token: String? = null) : RecyclerView.ViewHolder(b.root) {
        private var audioPlayer: MediaPlayer? = null
        private var videoPlayer: MediaPlayer? = null
        private val seekHandler = Handler(Looper.getMainLooper())
        private val audioHandler = Handler(Looper.getMainLooper())
        private var isSeeking = false
        private var audioSpeed = 1.0f
        private var audioWaveform: FloatArray = floatArrayOf()

        fun bind(msg: MessageDto, time: String, query: String = "", onReact: ((MessageDto, String) -> Unit)? = null, theme: ChatThemeManager.Theme = ChatThemeManager.Theme.DEFAULT) {
            b.layoutBubble.background = bubbleDrawable(itemView.context, theme.sentColor, true)
            b.tvTime.text = time
            if (msg.isRead) {
                b.tvReadStatus.text = "✓✓"
                b.tvReadStatus.setTextColor(itemView.context.getColor(R.color.accent_cyan))
            } else {
                b.tvReadStatus.text = "✓"
                b.tvReadStatus.setTextColor(0x99FFFFFF.toInt())
            }
            if (!msg.replyToSenderName.isNullOrBlank()) {
                b.layoutReply.visibility = View.VISIBLE
                b.tvReplySender.text = msg.replyToSenderName
                b.tvReplyText.text = msg.replyToText ?: "📎 Вложение"
            } else {
                b.layoutReply.visibility = View.GONE
            }
            bindReactions(b.layoutReactions, msg.reactions) { emoji -> onReact?.invoke(msg, emoji) }

            bindContent(msg, time, query)
        }

        private fun bindContent(msg: MessageDto, time: String, query: String = "") {
            val type = msg.attachmentType
            val url  = msg.attachmentUrl

            b.layoutBubble.visibility         = View.VISIBLE
            b.layoutVideoNote.visibility      = View.GONE
            b.tvMessage.visibility            = View.GONE
            b.layoutImage.visibility          = View.GONE
            b.layoutAudio.visibility          = View.GONE
            b.layoutFile.visibility           = View.GONE
            b.ivVideoNoteThumb.visibility     = View.VISIBLE
            b.textureVideoNote.visibility     = View.GONE
            b.videoNotePlayOverlay.visibility = View.VISIBLE
            b.layoutVideoSeek.visibility      = View.GONE
            videoPlayer?.release(); videoPlayer = null
            seekHandler.removeCallbacksAndMessages(null)
            audioHandler.removeCallbacksAndMessages(null)
            audioPlayer?.release(); audioPlayer = null

            when {
                type == "video_note" && url != null -> {
                    b.layoutBubble.visibility    = View.GONE
                    b.layoutVideoNote.visibility = View.VISIBLE
                    b.tvVideoNoteTime.text       = time
                    applyCircleClip(b.layoutVideoNoteCircle)
                    Glide.with(b.root).load(url).centerCrop().into(b.ivVideoNoteThumb)
                    b.layoutVideoNoteCircle.setOnClickListener { toggleVideoNote(url) }
                    setupSeekBar()
                }
                type == "image" && url != null -> {
                    b.layoutImage.visibility      = View.VISIBLE
                    b.videoPlayOverlay.visibility = View.GONE
                    Glide.with(b.root).load(url).centerCrop().into(b.ivAttachment)
                    b.layoutImage.setOnClickListener { openFileUrl(b.root, url) }
                }
                type == "video" && url != null -> {
                    b.layoutImage.visibility      = View.VISIBLE
                    b.videoPlayOverlay.visibility = View.VISIBLE
                    Glide.with(b.root).load(url).centerCrop().into(b.ivAttachment)
                    b.layoutImage.setOnClickListener { playVideoInApp(b.root, url) }
                }
                type == "audio" && url != null -> {
                    b.layoutAudio.visibility = View.VISIBLE
                    b.btnPlayAudio.text = "▶"
                    audioSpeed = 1.0f
                    b.btnAudioSpeed.text = "×1"
                    audioWaveform = WaveformView.pseudoWaveform(url.hashCode())
                    b.waveformAudio.amplitudes = audioWaveform
                    b.waveformAudio.progress = 0f
                    b.btnPlayAudio.setOnClickListener { toggleAudio(url) }
                    b.btnAudioSpeed.setOnClickListener { cycleSpeed() }
                }
                type == "file" && url != null -> {
                    b.layoutFile.visibility = View.VISIBLE
                    b.tvFileName.text = url.substringAfterLast("/")
                    b.layoutFile.setOnClickListener { openFileUrl(b.root, url) }
                }
                else -> {
                    b.tvMessage.visibility = View.VISIBLE
                    b.tvMessage.text = highlightQuery(msg.messageText ?: "", query)
                }
            }

            if (!msg.messageText.isNullOrBlank() && type != null && type != "video_note") {
                b.tvMessage.visibility = View.VISIBLE
                b.tvMessage.text = highlightQuery(msg.messageText, query)
            }
        }

        private fun cycleSpeed() {
            audioSpeed = when (audioSpeed) {
                1.0f -> 1.5f
                1.5f -> 2.0f
                else -> 1.0f
            }
            b.btnAudioSpeed.text = when (audioSpeed) {
                1.5f -> "×1.5"
                2.0f -> "×2"
                else -> "×1"
            }
            audioPlayer?.let { p ->
                try {
                    p.playbackParams = PlaybackParams().setSpeed(audioSpeed)
                } catch (_: Exception) {}
            }
        }

        private fun setupSeekBar() {
            b.seekbarVideoNote.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(sb: SeekBar) { isSeeking = true }
                override fun onStopTrackingTouch(sb: SeekBar) {
                    isSeeking = false
                    videoPlayer?.let { p ->
                        if (p.isPlaying || p.duration > 0) {
                            val pos = (sb.progress / 1000f * p.duration).toInt()
                            p.seekTo(pos)
                        }
                    }
                }
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        videoPlayer?.let { p ->
                            val secs = (progress / 1000f * p.duration / 1000).toInt()
                            b.tvVideoNoteDuration.text = "%d:%02d".format(secs / 60, secs % 60)
                        }
                    }
                }
            })
        }

        private fun startSeekUpdater() {
            seekHandler.removeCallbacksAndMessages(null)
            val update = object : Runnable {
                override fun run() {
                    val p = videoPlayer ?: return
                    if (!isSeeking && p.duration > 0) {
                        val progress = (p.currentPosition.toFloat() / p.duration * 1000).toInt()
                        b.seekbarVideoNote.progress = progress
                        val secs = p.currentPosition / 1000
                        b.tvVideoNoteDuration.text = "%d:%02d".format(secs / 60, secs % 60)
                    }
                    seekHandler.postDelayed(this, 250)
                }
            }
            seekHandler.post(update)
        }

        private fun startAudioProgressUpdater() {
            audioHandler.removeCallbacksAndMessages(null)
            val update = object : Runnable {
                override fun run() {
                    val p = audioPlayer ?: return
                    if (p.duration > 0) {
                        val progress = p.currentPosition.toFloat() / p.duration
                        b.waveformAudio.progress = progress
                        val secs = p.currentPosition / 1000
                        b.tvAudioDuration.text = "%d:%02d".format(secs / 60, secs % 60)
                    }
                    audioHandler.postDelayed(this, 100)
                }
            }
            audioHandler.post(update)
        }

        private fun toggleVideoNote(url: String) {
            if (videoPlayer?.isPlaying == true) { stopVideoNote(); return }
            b.videoNotePlayOverlay.tag = "loading"
            val ctx = b.root.context
            val uiHandler = Handler(Looper.getMainLooper())
            Thread {
                try {
                    val cacheFile = File(ctx.cacheDir, "vidnote_${url.hashCode()}.mp4")
                    if (!cacheFile.exists() || cacheFile.length() == 0L) {
                        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                        if (!token.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $token")
                        conn.connect()
                        if (conn.responseCode != 200) { uiHandler.post { resetThumb() }; return@Thread }
                        conn.inputStream.use { it.copyTo(cacheFile.outputStream()) }
                    }
                    uiHandler.post {
                        if (b.videoNotePlayOverlay.tag != "loading") return@post
                        b.videoNotePlayOverlay.tag = null
                        b.ivVideoNoteThumb.visibility     = View.GONE
                        b.videoNotePlayOverlay.visibility = View.GONE
                        b.textureVideoNote.visibility     = View.VISIBLE
                        b.layoutVideoSeek.visibility      = View.VISIBLE
                        applyCircleClip(b.textureVideoNote)
                        val path = cacheFile.absolutePath
                        if (b.textureVideoNote.isAvailable) {
                            startVideoPlayer(path, b.textureVideoNote.surfaceTexture!!)
                        } else {
                            b.textureVideoNote.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) = startVideoPlayer(path, st)
                                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean { videoPlayer?.release(); return true }
                                override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                            }
                        }
                    }
                } catch (_: Exception) { uiHandler.post { resetThumb() } }
            }.start()
        }

        private fun resetThumb() {
            b.videoNotePlayOverlay.tag        = null
            b.ivVideoNoteThumb.visibility     = View.VISIBLE
            b.videoNotePlayOverlay.visibility = View.VISIBLE
        }

        private fun startVideoPlayer(path: String, st: SurfaceTexture) {
            videoPlayer = MediaPlayer().apply {
                setSurface(Surface(st))
                setAudioAttributes(AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .setUsage(AudioAttributes.USAGE_MEDIA).build())
                setOnPreparedListener { p ->
                    p.isLooping = false
                    fixVideoAspect(p, b.textureVideoNote)
                    p.start()
                    b.seekbarVideoNote.max = 1000
                    startSeekUpdater()
                }
                setOnCompletionListener { stopVideoNote() }
                setOnErrorListener { _, _, _ -> stopVideoNote(); true }
                setDataSource(path)
                prepareAsync()
            }
        }

        private fun stopVideoNote() {
            seekHandler.removeCallbacksAndMessages(null)
            videoPlayer?.release(); videoPlayer = null
            b.videoNotePlayOverlay.tag        = null
            b.ivVideoNoteThumb.visibility     = View.VISIBLE
            b.videoNotePlayOverlay.visibility = View.VISIBLE
            b.textureVideoNote.visibility     = View.GONE
            b.layoutVideoSeek.visibility      = View.GONE
            b.seekbarVideoNote.progress       = 0
            b.tvVideoNoteDuration.text        = "0:00"
        }

        private fun toggleAudio(url: String) {
            if (audioPlayer?.isPlaying == true) {
                audioPlayer?.pause()
                audioHandler.removeCallbacksAndMessages(null)
                b.btnPlayAudio.text = "▶"
                releaseProximity(b.root)
                return
            }
            if (audioPlayer != null) {
                audioPlayer?.start()
                try { audioPlayer?.playbackParams = PlaybackParams().setSpeed(audioSpeed) } catch (_: Exception) {}
                b.btnPlayAudio.text = "⏸"
                startAudioProgressUpdater()
                return
            }
            b.btnPlayAudio.text = "⏸"
            b.waveformAudio.progress = 0f
            val ctx = b.root.context
            val handler = Handler(Looper.getMainLooper())
            Thread {
                try {
                    val cacheFile = File(ctx.cacheDir, "audio_${url.hashCode()}.tmp")
                    if (!cacheFile.exists() || cacheFile.length() == 0L) {
                        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                        if (!token.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $token")
                        conn.connect()
                        if (conn.responseCode != 200) { handler.post { b.btnPlayAudio.text = "▶" }; return@Thread }
                        conn.inputStream.use { it.copyTo(cacheFile.outputStream()) }
                    }
                    handler.post {
                        audioPlayer = MediaPlayer().apply {
                            setAudioAttributes(AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).build())
                            setVolume(1f, 1f)
                            setOnPreparedListener { p ->
                                try { p.playbackParams = PlaybackParams().setSpeed(audioSpeed) } catch (_: Exception) {}
                                p.start()
                                acquireProximity(b.root)
                                startAudioProgressUpdater()
                            }
                            setOnCompletionListener {
                                b.btnPlayAudio.text = "▶"
                                b.waveformAudio.progress = 0f
                                b.tvAudioDuration.text = "0:00"
                                audioHandler.removeCallbacksAndMessages(null)
                                audioPlayer = null
                                releaseProximity(b.root)
                            }
                            setOnErrorListener { _, _, _ ->
                                b.btnPlayAudio.text = "▶"
                                audioPlayer = null
                                releaseProximity(b.root)
                                true
                            }
                            setDataSource(cacheFile.absolutePath)
                            prepareAsync()
                        }
                    }
                } catch (_: Exception) { handler.post { b.btnPlayAudio.text = "▶" } }
            }.start()
        }
    }

    class ReceivedVH(private val b: ItemMessageReceivedBinding, private val token: String? = null) : RecyclerView.ViewHolder(b.root) {
        private var audioPlayer: MediaPlayer? = null
        private var videoPlayer: MediaPlayer? = null
        private val seekHandler = Handler(Looper.getMainLooper())
        private val audioHandler = Handler(Looper.getMainLooper())
        private var isSeeking = false
        private var audioSpeed = 1.0f
        private var audioWaveform: FloatArray = floatArrayOf()

        fun bind(msg: MessageDto, time: String, query: String = "", onReact: ((MessageDto, String) -> Unit)? = null, theme: ChatThemeManager.Theme = ChatThemeManager.Theme.DEFAULT) {
            b.layoutBubble.background = bubbleDrawable(itemView.context, theme.receivedColor, false)
            b.tvSenderName.text = msg.senderName
            b.tvTime.text = time
            if (!msg.replyToSenderName.isNullOrBlank()) {
                b.layoutReply.visibility = View.VISIBLE
                b.tvReplySender.text = msg.replyToSenderName
                b.tvReplyText.text = msg.replyToText ?: "📎 Вложение"
            } else {
                b.layoutReply.visibility = View.GONE
            }
            bindReactions(b.layoutReactions, msg.reactions) { emoji -> onReact?.invoke(msg, emoji) }

            bindContent(msg, time, query)
        }

        private fun bindContent(msg: MessageDto, time: String, query: String = "") {
            val type = msg.attachmentType
            val url  = msg.attachmentUrl

            b.layoutBubble.visibility         = View.VISIBLE
            b.layoutVideoNote.visibility      = View.GONE
            b.tvMessage.visibility            = View.GONE
            b.layoutImage.visibility          = View.GONE
            b.layoutAudio.visibility          = View.GONE
            b.layoutFile.visibility           = View.GONE
            b.ivVideoNoteThumb.visibility     = View.VISIBLE
            b.textureVideoNote.visibility     = View.GONE
            b.videoNotePlayOverlay.visibility = View.VISIBLE
            b.layoutVideoSeek.visibility      = View.GONE
            videoPlayer?.release(); videoPlayer = null
            seekHandler.removeCallbacksAndMessages(null)
            audioHandler.removeCallbacksAndMessages(null)
            audioPlayer?.release(); audioPlayer = null

            when {
                type == "video_note" && url != null -> {
                    b.layoutBubble.visibility    = View.GONE
                    b.layoutVideoNote.visibility = View.VISIBLE
                    b.tvVideoNoteTime.text       = time
                    applyCircleClip(b.layoutVideoNoteCircle)
                    Glide.with(b.root).load(url).centerCrop().into(b.ivVideoNoteThumb)
                    b.layoutVideoNoteCircle.setOnClickListener { toggleVideoNote(url) }
                    setupSeekBar()
                }
                type == "image" && url != null -> {
                    b.layoutImage.visibility      = View.VISIBLE
                    b.videoPlayOverlay.visibility = View.GONE
                    Glide.with(b.root).load(url).centerCrop().into(b.ivAttachment)
                    b.layoutImage.setOnClickListener { openFileUrl(b.root, url) }
                }
                type == "video" && url != null -> {
                    b.layoutImage.visibility      = View.VISIBLE
                    b.videoPlayOverlay.visibility = View.VISIBLE
                    Glide.with(b.root).load(url).centerCrop().into(b.ivAttachment)
                    b.layoutImage.setOnClickListener { playVideoInApp(b.root, url) }
                }
                type == "audio" && url != null -> {
                    b.layoutAudio.visibility = View.VISIBLE
                    b.btnPlayAudio.text = "▶"
                    audioSpeed = 1.0f
                    b.btnAudioSpeed.text = "×1"
                    audioWaveform = WaveformView.pseudoWaveform(url.hashCode())
                    b.waveformAudio.amplitudes = audioWaveform
                    b.waveformAudio.progress = 0f
                    b.btnPlayAudio.setOnClickListener { toggleAudio(url) }
                    b.btnAudioSpeed.setOnClickListener { cycleSpeed() }
                }
                type == "file" && url != null -> {
                    b.layoutFile.visibility = View.VISIBLE
                    b.tvFileName.text = url.substringAfterLast("/")
                    b.layoutFile.setOnClickListener { openFileUrl(b.root, url) }
                }
                else -> {
                    b.tvMessage.visibility = View.VISIBLE
                    b.tvMessage.text = highlightQuery(msg.messageText ?: "", query)
                }
            }

            if (!msg.messageText.isNullOrBlank() && type != null && type != "video_note") {
                b.tvMessage.visibility = View.VISIBLE
                b.tvMessage.text = highlightQuery(msg.messageText, query)
            }
        }

        private fun cycleSpeed() {
            audioSpeed = when (audioSpeed) {
                1.0f -> 1.5f
                1.5f -> 2.0f
                else -> 1.0f
            }
            b.btnAudioSpeed.text = when (audioSpeed) {
                1.5f -> "×1.5"
                2.0f -> "×2"
                else -> "×1"
            }
            audioPlayer?.let { p ->
                try {
                    p.playbackParams = PlaybackParams().setSpeed(audioSpeed)
                } catch (_: Exception) {}
            }
        }

        private fun setupSeekBar() {
            b.seekbarVideoNote.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(sb: SeekBar) { isSeeking = true }
                override fun onStopTrackingTouch(sb: SeekBar) {
                    isSeeking = false
                    videoPlayer?.let { p ->
                        if (p.isPlaying || p.duration > 0) {
                            val pos = (sb.progress / 1000f * p.duration).toInt()
                            p.seekTo(pos)
                        }
                    }
                }
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        videoPlayer?.let { p ->
                            val secs = (progress / 1000f * p.duration / 1000).toInt()
                            b.tvVideoNoteDuration.text = "%d:%02d".format(secs / 60, secs % 60)
                        }
                    }
                }
            })
        }

        private fun startSeekUpdater() {
            seekHandler.removeCallbacksAndMessages(null)
            val update = object : Runnable {
                override fun run() {
                    val p = videoPlayer ?: return
                    if (!isSeeking && p.duration > 0) {
                        val progress = (p.currentPosition.toFloat() / p.duration * 1000).toInt()
                        b.seekbarVideoNote.progress = progress
                        val secs = p.currentPosition / 1000
                        b.tvVideoNoteDuration.text = "%d:%02d".format(secs / 60, secs % 60)
                    }
                    seekHandler.postDelayed(this, 250)
                }
            }
            seekHandler.post(update)
        }

        private fun startAudioProgressUpdater() {
            audioHandler.removeCallbacksAndMessages(null)
            val update = object : Runnable {
                override fun run() {
                    val p = audioPlayer ?: return
                    if (p.duration > 0) {
                        val progress = p.currentPosition.toFloat() / p.duration
                        b.waveformAudio.progress = progress
                        val secs = p.currentPosition / 1000
                        b.tvAudioDuration.text = "%d:%02d".format(secs / 60, secs % 60)
                    }
                    audioHandler.postDelayed(this, 100)
                }
            }
            audioHandler.post(update)
        }

        private fun toggleVideoNote(url: String) {
            if (videoPlayer?.isPlaying == true) { stopVideoNote(); return }
            b.videoNotePlayOverlay.tag = "loading"
            val ctx = b.root.context
            val uiHandler = Handler(Looper.getMainLooper())
            Thread {
                try {
                    val cacheFile = File(ctx.cacheDir, "vidnote_${url.hashCode()}.mp4")
                    if (!cacheFile.exists() || cacheFile.length() == 0L) {
                        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                        if (!token.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $token")
                        conn.connect()
                        if (conn.responseCode != 200) { uiHandler.post { resetThumb() }; return@Thread }
                        conn.inputStream.use { it.copyTo(cacheFile.outputStream()) }
                    }
                    uiHandler.post {
                        if (b.videoNotePlayOverlay.tag != "loading") return@post
                        b.videoNotePlayOverlay.tag = null
                        b.ivVideoNoteThumb.visibility     = View.GONE
                        b.videoNotePlayOverlay.visibility = View.GONE
                        b.textureVideoNote.visibility     = View.VISIBLE
                        b.layoutVideoSeek.visibility      = View.VISIBLE
                        applyCircleClip(b.textureVideoNote)
                        val path = cacheFile.absolutePath
                        if (b.textureVideoNote.isAvailable) {
                            startVideoPlayer(path, b.textureVideoNote.surfaceTexture!!)
                        } else {
                            b.textureVideoNote.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) = startVideoPlayer(path, st)
                                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean { videoPlayer?.release(); return true }
                                override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                            }
                        }
                    }
                } catch (_: Exception) { uiHandler.post { resetThumb() } }
            }.start()
        }

        private fun resetThumb() {
            b.videoNotePlayOverlay.tag        = null
            b.ivVideoNoteThumb.visibility     = View.VISIBLE
            b.videoNotePlayOverlay.visibility = View.VISIBLE
        }

        private fun startVideoPlayer(path: String, st: SurfaceTexture) {
            videoPlayer = MediaPlayer().apply {
                setSurface(Surface(st))
                setAudioAttributes(AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .setUsage(AudioAttributes.USAGE_MEDIA).build())
                setOnPreparedListener { p ->
                    p.isLooping = false
                    fixVideoAspect(p, b.textureVideoNote)
                    p.start()
                    b.seekbarVideoNote.max = 1000
                    startSeekUpdater()
                }
                setOnCompletionListener { stopVideoNote() }
                setOnErrorListener { _, _, _ -> stopVideoNote(); true }
                setDataSource(path)
                prepareAsync()
            }
        }

        private fun stopVideoNote() {
            seekHandler.removeCallbacksAndMessages(null)
            videoPlayer?.release(); videoPlayer = null
            b.videoNotePlayOverlay.tag        = null
            b.ivVideoNoteThumb.visibility     = View.VISIBLE
            b.videoNotePlayOverlay.visibility = View.VISIBLE
            b.textureVideoNote.visibility     = View.GONE
            b.layoutVideoSeek.visibility      = View.GONE
            b.seekbarVideoNote.progress       = 0
            b.tvVideoNoteDuration.text        = "0:00"
        }

        private fun toggleAudio(url: String) {
            if (audioPlayer?.isPlaying == true) {
                audioPlayer?.pause()
                audioHandler.removeCallbacksAndMessages(null)
                b.btnPlayAudio.text = "▶"
                releaseProximity(b.root)
                return
            }
            if (audioPlayer != null) {
                audioPlayer?.start()
                try { audioPlayer?.playbackParams = PlaybackParams().setSpeed(audioSpeed) } catch (_: Exception) {}
                b.btnPlayAudio.text = "⏸"
                startAudioProgressUpdater()
                return
            }
            b.btnPlayAudio.text = "⏸"
            b.waveformAudio.progress = 0f
            val ctx = b.root.context
            val handler = Handler(Looper.getMainLooper())
            Thread {
                try {
                    val cacheFile = File(ctx.cacheDir, "audio_${url.hashCode()}.tmp")
                    if (!cacheFile.exists() || cacheFile.length() == 0L) {
                        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                        if (!token.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $token")
                        conn.connect()
                        if (conn.responseCode != 200) { handler.post { b.btnPlayAudio.text = "▶" }; return@Thread }
                        conn.inputStream.use { it.copyTo(cacheFile.outputStream()) }
                    }
                    handler.post {
                        audioPlayer = MediaPlayer().apply {
                            setAudioAttributes(AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).build())
                            setVolume(1f, 1f)
                            setOnPreparedListener { p ->
                                try { p.playbackParams = PlaybackParams().setSpeed(audioSpeed) } catch (_: Exception) {}
                                p.start()
                                acquireProximity(b.root)
                                startAudioProgressUpdater()
                            }
                            setOnCompletionListener {
                                b.btnPlayAudio.text = "▶"
                                b.waveformAudio.progress = 0f
                                b.tvAudioDuration.text = "0:00"
                                audioHandler.removeCallbacksAndMessages(null)
                                audioPlayer = null
                                releaseProximity(b.root)
                            }
                            setOnErrorListener { _, _, _ ->
                                b.btnPlayAudio.text = "▶"
                                audioPlayer = null
                                releaseProximity(b.root)
                                true
                            }
                            setDataSource(cacheFile.absolutePath)
                            prepareAsync()
                        }
                    }
                } catch (_: Exception) { handler.post { b.btnPlayAudio.text = "▶" } }
            }.start()
        }
    }

    companion object {

        fun bindReactions(container: android.widget.LinearLayout, reactions: List<com.fitnesstrainer.app.data.model.ReactionDto>?, onReact: ((String) -> Unit)? = null) {
            container.removeAllViews()
            if (reactions.isNullOrEmpty()) { container.visibility = View.GONE; return }
            container.visibility = View.VISIBLE
            val ctx = container.context
            val dp4 = (4 * ctx.resources.displayMetrics.density).toInt()
            val dp8 = (8 * ctx.resources.displayMetrics.density).toInt()
            reactions.forEach { r ->
                val tv = android.widget.TextView(ctx).apply {
                    text = "${r.emoji} ${r.count}"
                    textSize = 12f
                    setTextColor(if (r.myReaction) Color.parseColor("#89b4fa") else Color.parseColor("#CCCCCC"))
                    setPadding(dp8, dp4, dp8, dp4)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 40f
                        setColor(if (r.myReaction) Color.parseColor("#1A2A4A") else Color.parseColor("#1A1A2E"))
                        setStroke(2, if (r.myReaction) Color.parseColor("#4488FF") else Color.parseColor("#2A2D3E"))
                    }
                    val lp = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    lp.marginEnd = dp4
                    layoutParams = lp
                    setOnClickListener { onReact?.invoke(r.emoji) }
                }
                container.addView(tv)
            }
        }

        fun highlightQuery(text: String, query: String): CharSequence {
            if (query.isBlank() || text.isBlank()) return text
            val spannable = SpannableString(text)
            val lowerText = text.lowercase()
            val lowerQuery = query.lowercase()
            var index = lowerText.indexOf(lowerQuery)
            while (index >= 0) {
                spannable.setSpan(
                    BackgroundColorSpan(Color.parseColor("#4D7EB0FF")),
                    index, index + query.length,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                index = lowerText.indexOf(lowerQuery, index + 1)
            }
            return spannable
        }

        fun bubbleDrawable(ctx: android.content.Context, color: Int, isSent: Boolean): GradientDrawable {
            val dp = ctx.resources.displayMetrics.density
            val r = 16f * dp
            val small = 4f * dp
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(color)
                cornerRadii = if (isSent)
                    floatArrayOf(r, r, r, r, r, r, small, small)
                else
                    floatArrayOf(r, r, r, r, small, small, r, r)
            }
        }

        private val DIFF = object : DiffUtil.ItemCallback<MessageDto>() {
            override fun areItemsTheSame(a: MessageDto, b: MessageDto) = a.messageId == b.messageId
            override fun areContentsTheSame(a: MessageDto, b: MessageDto) = a == b
        }

        fun formatTime(sentAt: String) = try {
            OffsetDateTime.parse(sentAt, DateTimeFormatter.ISO_DATE_TIME)
                .atZoneSameInstant(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(sentAt, DateTimeFormatter.ISO_DATE_TIME)
                    .format(DateTimeFormatter.ofPattern("HH:mm"))
            } catch (_: Exception) { "" }
        }

        fun playVideoInApp(view: View, url: String) {
            val ctx = view.context
            val dialog = Dialog(ctx, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            val videoView = VideoView(ctx)
            videoView.setVideoURI(Uri.parse(url))
            dialog.setContentView(videoView)
            dialog.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            videoView.setOnPreparedListener { it.isLooping = false; videoView.start() }
            videoView.setOnCompletionListener { dialog.dismiss() }
            dialog.setOnDismissListener { videoView.stopPlayback() }
            dialog.show()
        }

        fun openFileUrl(view: View, url: String) {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
            view.context.startActivity(intent)
        }

        fun applyCircleClip(view: View) {
            view.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, outline: Outline) {
                    outline.setOval(0, 0, v.width, v.height)
                }
            }
            view.clipToOutline = true
        }

        fun fixVideoAspect(player: MediaPlayer, tv: TextureView) {
            val vW = player.videoWidth.takeIf { it > 0 } ?: return
            val vH = player.videoHeight.takeIf { it > 0 } ?: return
            val tW = tv.width.takeIf { it > 0 }
                ?: (tv.layoutParams?.width?.takeIf { it > 0 }) ?: return
            val tH = tv.height.takeIf { it > 0 }
                ?: (tv.layoutParams?.height?.takeIf { it > 0 }) ?: return
            val maxScale = maxOf(tW.toFloat() / vW, tH.toFloat() / vH)
            val scaleX = maxScale * vW / tW
            val scaleY = maxScale * vH / tH
            val matrix = android.graphics.Matrix()
            matrix.setScale(scaleX, scaleY, tW / 2f, tH / 2f)
            tv.setTransform(matrix)
        }

        fun acquireProximity(view: View) {
            val am = view.context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
            am.mode = AudioManager.MODE_IN_COMMUNICATION
        }

        fun releaseProximity(view: View) {
            val am = view.context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
            am.mode = AudioManager.MODE_NORMAL
        }
    }
}
