package com.fitnesstrainer.app.ui.chat

import android.app.Dialog
import android.graphics.Outline
import android.graphics.SurfaceTexture
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
import com.fitnesstrainer.app.data.model.MessageDto
import com.fitnesstrainer.app.databinding.ItemMessageReceivedBinding
import com.fitnesstrainer.app.databinding.ItemMessageSentBinding
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val TYPE_SENT     = 0
private const val TYPE_RECEIVED = 1

class ChatAdapter(
    private val myUserId: Int,
    private val accessToken: String? = null
) : ListAdapter<MessageDto, RecyclerView.ViewHolder>(DIFF) {

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
            is SentVH     -> holder.bind(msg, time)
            is ReceivedVH -> holder.bind(msg, time)
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

        fun bind(msg: MessageDto, time: String) {
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
                    b.tvMessage.text = msg.messageText ?: ""
                }
            }

            if (!msg.messageText.isNullOrBlank() && type != null && type != "video_note") {
                b.tvMessage.visibility = View.VISIBLE
                b.tvMessage.text = msg.messageText
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
                    b.tvMessage.text = msg.messageText ?: ""
                }
            }

            if (!msg.messageText.isNullOrBlank() && type != null && type != "video_note") {
                b.tvMessage.visibility = View.VISIBLE
                b.tvMessage.text = msg.messageText
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
