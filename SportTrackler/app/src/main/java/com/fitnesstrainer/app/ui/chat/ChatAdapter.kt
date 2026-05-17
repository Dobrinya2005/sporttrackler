package com.fitnesstrainer.app.ui.chat

import android.app.Dialog
import android.graphics.Outline
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
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
import android.widget.VideoView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fitnesstrainer.app.data.model.MessageDto
import com.fitnesstrainer.app.databinding.ItemMessageReceivedBinding
import com.fitnesstrainer.app.databinding.ItemMessageSentBinding
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val TYPE_SENT     = 0
private const val TYPE_RECEIVED = 1

class ChatAdapter(
    private val myUserId: Int
) : ListAdapter<MessageDto, RecyclerView.ViewHolder>(DIFF) {

    override fun getItemViewType(position: Int) =
        if (getItem(position).senderId == myUserId) TYPE_SENT else TYPE_RECEIVED

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_SENT)
            SentVH(ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        else
            ReceivedVH(ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg  = getItem(position)
        val time = formatTime(msg.sentAt)
        when (holder) {
            is SentVH     -> holder.bind(msg, time)
            is ReceivedVH -> holder.bind(msg, time)
        }
    }

    class SentVH(private val b: ItemMessageSentBinding) : RecyclerView.ViewHolder(b.root) {
        private var audioPlayer: MediaPlayer? = null
        private var videoPlayer: MediaPlayer? = null

        fun bind(msg: MessageDto, time: String) {
            b.tvTime.text = time
            bindContent(msg, time)
        }

        private fun bindContent(msg: MessageDto, time: String) {
            val type = msg.attachmentType
            val url  = msg.attachmentUrl

            // Reset all
            b.layoutBubble.visibility        = View.VISIBLE
            b.layoutVideoNote.visibility     = View.GONE
            b.tvMessage.visibility           = View.GONE
            b.layoutImage.visibility         = View.GONE
            b.layoutAudio.visibility         = View.GONE
            b.layoutFile.visibility          = View.GONE
            b.ivVideoNoteThumb.visibility    = View.VISIBLE
            b.textureVideoNote.visibility    = View.GONE
            b.videoNotePlayOverlay.visibility = View.VISIBLE
            videoPlayer?.release(); videoPlayer = null

            when {
                type == "video_note" && url != null -> {
                    b.layoutBubble.visibility    = View.GONE
                    b.layoutVideoNote.visibility = View.VISIBLE
                    b.tvVideoNoteTime.text       = time
                    applyCircleClip(b.layoutVideoNote)
                    Glide.with(b.root).load(url).centerCrop().into(b.ivVideoNoteThumb)
                    b.layoutVideoNote.setOnClickListener { toggleVideoNote(url) }
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
                    b.btnPlayAudio.setOnClickListener { toggleAudio(url) }
                }
                type == "file" && url != null -> {
                    b.layoutFile.visibility = View.VISIBLE
                    b.tvFileName.text = url.substringAfterLast("/")
                    b.layoutFile.setOnClickListener { openFileUrl(b.root, url) }
                }
                else -> {
                    b.tvMessage.visibility = View.VISIBLE
                    b.tvMessage.text = msg.messageText ?: ""
                }
            }

            if (!msg.messageText.isNullOrBlank() && type != null && type != "video_note") {
                b.tvMessage.visibility = View.VISIBLE
                b.tvMessage.text = msg.messageText
            }
        }

        private fun toggleVideoNote(url: String) {
            if (videoPlayer?.isPlaying == true) { stopVideoNote(); return }
            b.ivVideoNoteThumb.visibility     = View.GONE
            b.videoNotePlayOverlay.visibility = View.GONE
            b.textureVideoNote.visibility     = View.VISIBLE
            applyCircleClip(b.textureVideoNote)
            if (b.textureVideoNote.isAvailable) {
                startVideoPlayer(url, b.textureVideoNote.surfaceTexture!!)
            } else {
                b.textureVideoNote.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) = startVideoPlayer(url, st)
                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean { videoPlayer?.release(); return true }
                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                }
            }
        }

        private fun startVideoPlayer(url: String, st: SurfaceTexture) {
            videoPlayer = MediaPlayer().apply {
                setSurface(Surface(st))
                setAudioAttributes(AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .setUsage(AudioAttributes.USAGE_MEDIA).build())
                setOnPreparedListener { it.isLooping = false; it.start() }
                setOnCompletionListener { stopVideoNote() }
                setOnErrorListener { _, _, _ -> stopVideoNote(); true }
                setDataSource(b.root.context, Uri.parse(url))
                prepareAsync()
            }
        }

        private fun stopVideoNote() {
            videoPlayer?.release(); videoPlayer = null
            b.ivVideoNoteThumb.visibility     = View.VISIBLE
            b.videoNotePlayOverlay.visibility = View.VISIBLE
            b.textureVideoNote.visibility     = View.GONE
        }

        private fun toggleAudio(url: String) {
            if (audioPlayer?.isPlaying == true) { audioPlayer?.pause(); b.btnPlayAudio.text = "▶"; return }
            if (audioPlayer != null) { audioPlayer?.start(); b.btnPlayAudio.text = "⏸"; return }
            b.btnPlayAudio.text = "⏸"
            val ctx = b.root.context
            val handler = Handler(Looper.getMainLooper())
            Thread {
                try {
                    val cacheFile = File(ctx.cacheDir, "audio_${url.hashCode()}.tmp")
                    if (!cacheFile.exists() || cacheFile.length() == 0L)
                        java.net.URL(url).openStream().use { it.copyTo(cacheFile.outputStream()) }
                    handler.post {
                        audioPlayer = MediaPlayer().apply {
                            setAudioAttributes(AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA).build())
                            setOnPreparedListener { start() }
                            setOnCompletionListener { b.btnPlayAudio.text = "▶"; audioPlayer = null }
                            setOnErrorListener { _, _, _ -> b.btnPlayAudio.text = "▶"; audioPlayer = null; true }
                            setDataSource(cacheFile.absolutePath)
                            prepareAsync()
                        }
                    }
                } catch (_: Exception) { handler.post { b.btnPlayAudio.text = "▶" } }
            }.start()
        }
    }

    class ReceivedVH(private val b: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(b.root) {
        private var audioPlayer: MediaPlayer? = null
        private var videoPlayer: MediaPlayer? = null

        fun bind(msg: MessageDto, time: String) {
            b.tvSenderName.text = msg.senderName
            b.tvTime.text = time
            bindContent(msg, time)
        }

        private fun bindContent(msg: MessageDto, time: String) {
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
            videoPlayer?.release(); videoPlayer = null

            when {
                type == "video_note" && url != null -> {
                    b.layoutBubble.visibility    = View.GONE
                    b.layoutVideoNote.visibility = View.VISIBLE
                    b.tvVideoNoteTime.text       = time
                    applyCircleClip(b.layoutVideoNote)
                    Glide.with(b.root).load(url).centerCrop().into(b.ivVideoNoteThumb)
                    b.layoutVideoNote.setOnClickListener { toggleVideoNote(url) }
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
                    b.btnPlayAudio.setOnClickListener { toggleAudio(url) }
                }
                type == "file" && url != null -> {
                    b.layoutFile.visibility = View.VISIBLE
                    b.tvFileName.text = url.substringAfterLast("/")
                    b.layoutFile.setOnClickListener { openFileUrl(b.root, url) }
                }
                else -> {
                    b.tvMessage.visibility = View.VISIBLE
                    b.tvMessage.text = msg.messageText ?: ""
                }
            }

            if (!msg.messageText.isNullOrBlank() && type != null && type != "video_note") {
                b.tvMessage.visibility = View.VISIBLE
                b.tvMessage.text = msg.messageText
            }
        }

        private fun toggleVideoNote(url: String) {
            if (videoPlayer?.isPlaying == true) { stopVideoNote(); return }
            b.ivVideoNoteThumb.visibility     = View.GONE
            b.videoNotePlayOverlay.visibility = View.GONE
            b.textureVideoNote.visibility     = View.VISIBLE
            applyCircleClip(b.textureVideoNote)
            if (b.textureVideoNote.isAvailable) {
                startVideoPlayer(url, b.textureVideoNote.surfaceTexture!!)
            } else {
                b.textureVideoNote.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) = startVideoPlayer(url, st)
                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean { videoPlayer?.release(); return true }
                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                }
            }
        }

        private fun startVideoPlayer(url: String, st: SurfaceTexture) {
            videoPlayer = MediaPlayer().apply {
                setSurface(Surface(st))
                setAudioAttributes(AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .setUsage(AudioAttributes.USAGE_MEDIA).build())
                setOnPreparedListener { it.isLooping = false; it.start() }
                setOnCompletionListener { stopVideoNote() }
                setOnErrorListener { _, _, _ -> stopVideoNote(); true }
                setDataSource(b.root.context, Uri.parse(url))
                prepareAsync()
            }
        }

        private fun stopVideoNote() {
            videoPlayer?.release(); videoPlayer = null
            b.ivVideoNoteThumb.visibility     = View.VISIBLE
            b.videoNotePlayOverlay.visibility = View.VISIBLE
            b.textureVideoNote.visibility     = View.GONE
        }

        private fun toggleAudio(url: String) {
            if (audioPlayer?.isPlaying == true) { audioPlayer?.pause(); b.btnPlayAudio.text = "▶"; return }
            if (audioPlayer != null) { audioPlayer?.start(); b.btnPlayAudio.text = "⏸"; return }
            b.btnPlayAudio.text = "⏸"
            val ctx = b.root.context
            val handler = Handler(Looper.getMainLooper())
            Thread {
                try {
                    val cacheFile = File(ctx.cacheDir, "audio_${url.hashCode()}.tmp")
                    if (!cacheFile.exists() || cacheFile.length() == 0L)
                        java.net.URL(url).openStream().use { it.copyTo(cacheFile.outputStream()) }
                    handler.post {
                        audioPlayer = MediaPlayer().apply {
                            setAudioAttributes(AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA).build())
                            setOnPreparedListener { start() }
                            setOnCompletionListener { b.btnPlayAudio.text = "▶"; audioPlayer = null }
                            setOnErrorListener { _, _, _ -> b.btnPlayAudio.text = "▶"; audioPlayer = null; true }
                            setDataSource(cacheFile.absolutePath)
                            prepareAsync()
                        }
                    }
                } catch (_: Exception) { handler.post { b.btnPlayAudio.text = "▶" } }
            }.start()
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MessageDto>() {
            override fun areItemsTheSame(a: MessageDto, b: MessageDto) = a.messageId == b.messageId
            override fun areContentsTheSame(a: MessageDto, b: MessageDto) = a == b
        }

        fun formatTime(sentAt: String) = try {
            LocalDateTime.parse(sentAt, DateTimeFormatter.ISO_DATE_TIME)
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) { "" }

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
    }
}
