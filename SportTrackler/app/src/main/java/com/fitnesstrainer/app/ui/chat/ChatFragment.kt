package com.fitnesstrainer.app.ui.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.databinding.FragmentChatBinding
import java.io.File
import java.io.FileOutputStream

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val args: ChatFragmentArgs by navArgs()
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ChatAdapter
    private var infoVisible = false

    private val typingStopHandler = Handler(Looper.getMainLooper())
    private val typingStopRunnable = Runnable { viewModel.stopTyping() }

    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private val proximityListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (event.values[0] < (proximitySensor?.maximumRange ?: 5f)) {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = false
            } else {
                audioManager.mode = AudioManager.MODE_NORMAL
                audioManager.isSpeakerphoneOn = false
            }
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    private var recordingSeconds = 0
    private var isRecordingLocked = false
    private var isPaused = false
    private var micStartRawY = 0f
    private var micStartRawX = 0f
    private val SWIPE_THRESHOLD_PX get() = 80 * resources.displayMetrics.density

    private val recordingHandler = Handler(Looper.getMainLooper())
    private val recordingTicker = object : Runnable {
        override fun run() {
            if (!isPaused) {
                recordingSeconds++
                val m = recordingSeconds / 60
                val s = recordingSeconds % 60
                val timeStr = "%d:%02d".format(m, s)
                binding.tvRecordingTime.text = timeStr
                binding.tvRecordingTimeLocked.text = timeStr
            }
            recordingHandler.postDelayed(this, 1000)
        }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        sendUriAsAttachment(uri)
    }

    private val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        sendUriAsAttachment(uri)
    }

    private var pendingVideoUri: Uri? = null
    private val captureVideo = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { saved ->
        if (saved) {
            pendingVideoUri?.let { sendUriAsAttachment(it) }
        }
        pendingVideoUri = null
    }

    private var pendingCameraAction: (() -> Unit)? = null
    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingCameraAction?.invoke()
        else Toast.makeText(requireContext(), "Нет разрешения на камеру", Toast.LENGTH_SHORT).show()
        pendingCameraAction = null
    }

    private val requestAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) Toast.makeText(requireContext(), "Нет разрешения на микрофон", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvContactName.text = args.contactName
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.btnInfo.setOnClickListener {
            infoVisible = !infoVisible
            binding.cardContactInfo.visibility = if (infoVisible) View.VISIBLE else View.GONE
        }

        val layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        binding.rvMessages.layoutManager = layoutManager

        viewModel.myUserId.observe(viewLifecycleOwner) { myId ->
            if (myId != -1) {
                adapter = ChatAdapter(myId, viewModel.accessToken.value)
                binding.rvMessages.adapter = adapter
            }
        }

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            if (::adapter.isInitialized) {
                adapter.submitList(messages) {
                    if (messages.isNotEmpty())
                        binding.rvMessages.scrollToPosition(messages.size - 1)
                }
            }
        }

        viewModel.contactProfile.observe(viewLifecycleOwner) { profile ->
            if (profile != null) {
                binding.btnInfo.visibility = View.VISIBLE
                binding.tvInfoName.text = "${profile.firstName} ${profile.lastName}"
                binding.tvInfoEmail.text = profile.email
                binding.tvInfoEmailShort.text = profile.email
                binding.tvInfoPhone.text = profile.phone ?: "—"
                if (!profile.avatarUrl.isNullOrBlank()) {
                    Glide.with(this).load(profile.avatarUrl)
                        .placeholder(R.drawable.ic_person).circleCrop().into(binding.ivContactAvatar)
                }
            } else {
                binding.btnInfo.visibility = View.GONE
            }
        }

        viewModel.connectionState.observe(viewLifecycleOwner) { connected ->
            if (!connected) {
                val tv = binding.tvConnectionStatus
                tv.visibility = View.VISIBLE
                tv.text = "● нет соединения"
                tv.setTextColor(android.graphics.Color.parseColor("#FF4777"))
            }
            // Когда connected=true, статус обновит contactStatus observer
        }

        viewModel.contactStatus.observe(viewLifecycleOwner) { status ->
            val tv = binding.tvConnectionStatus
            tv.visibility = View.VISIBLE
            if (status.isOnline) {
                tv.text = "● в сети"
                tv.setTextColor(android.graphics.Color.parseColor("#00E596"))
            } else {
                val lastSeenText = formatLastSeen(status.lastSeen)
                tv.text = lastSeenText
                tv.setTextColor(android.graphics.Color.parseColor("#888888"))
            }
        }

        viewModel.uploadError.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrBlank())
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
        }

        viewModel.recordingError.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrBlank()) {
                recordingHandler.removeCallbacks(recordingTicker)
                isRecordingLocked = false
                isPaused = false
                binding.recordingPanel.visibility = View.GONE
                binding.inputPanel.visibility = View.VISIBLE
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.typingStatus.observe(viewLifecycleOwner) { typing ->
            val tv = binding.tvConnectionStatus
            if (!typing.isNullOrBlank()) {
                tv.visibility = View.VISIBLE
                tv.text = typing
                tv.setTextColor(android.graphics.Color.parseColor("#89b4fa"))
            } else {
                // восстанавливаем статус онлайн/оффлайн
                viewModel.contactStatus.value?.let { status ->
                    if (status.isOnline) {
                        tv.text = "● в сети"
                        tv.setTextColor(android.graphics.Color.parseColor("#00E596"))
                    } else {
                        tv.text = formatLastSeen(status.lastSeen)
                        tv.setTextColor(android.graphics.Color.parseColor("#888888"))
                    }
                }
            }
        }

        binding.btnVideoNote.setOnClickListener { openVideoNoteRecorder() }

        // Show send/mic/vidnote based on text content
        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val hasText = !s.isNullOrBlank()
                binding.btnSend.visibility      = if (hasText) View.VISIBLE else View.GONE
                binding.btnMic.visibility       = if (hasText) View.GONE  else View.VISIBLE
                binding.btnVideoNote.visibility = if (hasText) View.GONE  else View.VISIBLE
                if (hasText) {
                    viewModel.sendTyping("text")
                    typingStopHandler.removeCallbacks(typingStopRunnable)
                    typingStopHandler.postDelayed(typingStopRunnable, 3000)
                } else {
                    typingStopHandler.removeCallbacks(typingStopRunnable)
                    viewModel.stopTyping()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                typingStopHandler.removeCallbacks(typingStopRunnable)
                viewModel.stopTyping()
                viewModel.sendMessage(text)
                binding.etMessage.setText("")
            }
        }

        binding.btnAttach.setOnClickListener { showAttachmentMenu() }

        // Highlight attach icon on focus
        binding.etMessage.setOnFocusChangeListener { _, focused ->
            binding.btnAttach.setColorFilter(
                if (focused) requireContext().getColor(com.fitnesstrainer.app.R.color.accent_cyan)
                else requireContext().getColor(com.fitnesstrainer.app.R.color.text_hint)
            )
        }

        // Hold-to-record mic with swipe gestures
        binding.btnMic.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    micStartRawX = event.rawX
                    micStartRawY = event.rawY
                    startRecording()
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isRecordingLocked) {
                        val dx = event.rawX - micStartRawX
                        val dy = event.rawY - micStartRawY
                        when {
                            dy < -SWIPE_THRESHOLD_PX -> lockRecording()
                            dx < -SWIPE_THRESHOLD_PX -> cancelRecording()
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (!isRecordingLocked) stopRecordingAndSend()
                }
                MotionEvent.ACTION_CANCEL -> {
                    if (!isRecordingLocked) cancelRecording()
                }
            }
            true
        }

        binding.btnRecordDelete.setOnClickListener { cancelRecording() }
        binding.btnRecordSend.setOnClickListener   { stopRecordingAndSend() }
        binding.btnRecordPause.setOnClickListener  { togglePause() }

        viewModel.amplitudes.observe(viewLifecycleOwner) { amps ->
            if (amps.isNotEmpty()) {
                binding.waveformRecording.amplitudes = amps
                binding.waveformRecordingLocked.amplitudes = amps
            }
        }

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        sensorManager?.registerListener(proximityListener, proximitySensor, SensorManager.SENSOR_DELAY_UI)

        viewModel.init(args.contactId)
    }

    private fun showAttachmentMenu() {
        val options = arrayOf(
            "🖼️  Фото из галереи",
            "🎬  Видео из галереи",
            "📹  Записать видео",
            "📁  Файл"
        )
        AlertDialog.Builder(requireContext())
            .setTitle("Вложение")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickMedia.launch("image/*")
                    1 -> pickMedia.launch("video/*")
                    2 -> checkCameraAndRecord()
                    3 -> pickFile.launch("*/*")
                }
            }
            .show()
    }

    private fun checkCameraAndRecord() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            pendingCameraAction = { launchVideoCapture() }
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        } else {
            launchVideoCapture()
        }
    }

    private fun openVideoNoteRecorder() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            pendingCameraAction = { openVideoNoteRecorder() }
            requestCameraPermission.launch(Manifest.permission.CAMERA)
            return
        }
        val sheet = VideoNoteBottomSheet()
        sheet.onVideoReady = { file ->
            viewModel.stopTyping()
            viewModel.sendMediaFile(file, "video/mp4", "video_note")
        }
        viewModel.sendTyping("video")
        sheet.show(parentFragmentManager, "video_note")
    }

    private fun launchVideoCapture() {
        val videoFile = File(requireContext().externalCacheDir ?: requireContext().cacheDir,
            "video_${System.currentTimeMillis()}.mp4")
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            videoFile
        )
        pendingVideoUri = uri
        captureVideo.launch(uri)
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        isRecordingLocked = false
        isPaused = false
        recordingSeconds = 0
        val outFile = File(requireContext().cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        viewModel.startVoiceRecording(outFile)
        binding.inputPanel.visibility          = View.GONE
        binding.recordingPanel.visibility      = View.VISIBLE
        binding.layoutHoldHints.visibility     = View.VISIBLE
        binding.layoutLockedControls.visibility = View.GONE
        binding.tvRecordingTime.text = "0:00"
        recordingHandler.post(recordingTicker)
        viewModel.sendTyping("voice")
    }

    private fun lockRecording() {
        if (isRecordingLocked) return
        isRecordingLocked = true
        binding.layoutHoldHints.visibility      = View.GONE
        binding.layoutLockedControls.visibility = View.VISIBLE
        binding.tvRecordingTimeLocked.text = binding.tvRecordingTime.text
        binding.btnRecordPause.setImageResource(R.drawable.ic_pause)
    }

    private fun togglePause() {
        isPaused = !isPaused
        if (isPaused) {
            viewModel.pauseVoiceRecording()
            binding.btnRecordPause.setImageResource(R.drawable.ic_play)
            binding.tvRecordDotLocked.text = "⚪"
        } else {
            viewModel.resumeVoiceRecording()
            binding.btnRecordPause.setImageResource(R.drawable.ic_pause)
            binding.tvRecordDotLocked.text = "🔴"
        }
    }

    private fun stopRecordingAndSend() {
        recordingHandler.removeCallbacks(recordingTicker)
        isRecordingLocked = false
        isPaused = false
        binding.recordingPanel.visibility = View.GONE
        binding.inputPanel.visibility     = View.VISIBLE
        viewModel.stopTyping()
        viewModel.stopVoiceRecordingAndSend()
    }

    private fun cancelRecording() {
        recordingHandler.removeCallbacks(recordingTicker)
        isRecordingLocked = false
        isPaused = false
        binding.recordingPanel.visibility = View.GONE
        binding.inputPanel.visibility     = View.VISIBLE
        viewModel.stopTyping()
        viewModel.cancelVoiceRecording()
    }

    private fun sendUriAsAttachment(uri: Uri) {
        val ctx = requireContext()
        try {
            val mimeType = ctx.contentResolver.getType(uri) ?: "application/octet-stream"
            val fileName = getFileName(ctx.contentResolver, uri) ?: "attachment"
            val file     = File(ctx.cacheDir, fileName)
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { input.copyTo(it) }
            }
            viewModel.sendMediaFile(file, mimeType)
        } catch (e: Exception) {
            Toast.makeText(ctx, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(resolver: ContentResolver, uri: Uri): String? {
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) return cursor.getString(idx)
        }
        return uri.lastPathSegment
    }

    override fun onStart() {
        super.onStart()
        viewModel.reconnectHub()
    }

    override fun onStop() {
        super.onStop()
        viewModel.disconnectHub()
    }

    override fun onDestroyView() {
        recordingHandler.removeCallbacks(recordingTicker)
        typingStopHandler.removeCallbacks(typingStopRunnable)
        sensorManager?.unregisterListener(proximityListener)
        val audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.mode != AudioManager.MODE_NORMAL) audioManager.mode = AudioManager.MODE_NORMAL
        super.onDestroyView()
        _binding = null
    }

    private fun formatLastSeen(lastSeen: String?): String {
        if (lastSeen.isNullOrBlank()) return "● не в сети"
        return try {
            val dt = java.time.OffsetDateTime.parse(lastSeen)
            val local = dt.toLocalDateTime()
            val now = java.time.LocalDateTime.now()
            val time = local.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            when {
                local.toLocalDate() == now.toLocalDate() -> "● был(а) в сети сегодня в $time"
                local.toLocalDate() == now.toLocalDate().minusDays(1) -> "● был(а) в сети вчера в $time"
                else -> "● был(а) в сети ${local.format(java.time.format.DateTimeFormatter.ofPattern("d MMM"))} в $time"
            }
        } catch (_: Exception) {
            "● не в сети"
        }
    }
}
