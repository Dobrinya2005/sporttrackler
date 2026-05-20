package com.fitnesstrainer.app.ui.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.databinding.FragmentGroupChatBinding
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream

class GroupChatFragment : Fragment() {

    private var _binding: FragmentGroupChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GroupChatViewModel by viewModels()
    private lateinit var adapter: GroupChatAdapter

    private var recordingSeconds = 0
    private var isRecordingLocked = false
    private var isPaused = false
    private var micStartRawX = 0f
    private var micStartRawY = 0f
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
        if (saved) pendingVideoUri?.let { sendUriAsAttachment(it) }
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGroupChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val groupId   = arguments?.getInt("groupId") ?: 0
        val groupName = arguments?.getString("groupName") ?: "Группа"
        val myUserId  = runBlocking { App.instance.tokenStorage.getUserId() }

        adapter = GroupChatAdapter(myUserId)

        binding.tvGroupName.text = groupName
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter

        viewModel.init(groupId)

        viewModel.accessToken.observe(viewLifecycleOwner) { token ->
            if (token != null) {
                adapter = GroupChatAdapter(myUserId, token)
                binding.rvMessages.adapter = adapter
                viewModel.messages.value?.let { msgs ->
                    adapter.submitList(msgs.toList())
                }
            }
        }

        viewModel.messages.observe(viewLifecycleOwner) { msgs ->
            adapter.submitList(msgs.toList())
            if (msgs.isNotEmpty()) binding.rvMessages.scrollToPosition(msgs.size - 1)
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
                binding.inputPanel.visibility     = View.VISIBLE
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.amplitudes.observe(viewLifecycleOwner) { amps ->
            if (amps.isNotEmpty()) {
                binding.waveformRecording.amplitudes = amps
                binding.waveformRecordingLocked.amplitudes = amps
            }
        }

        viewModel.group.observe(viewLifecycleOwner) { group ->
            if (group != null) {
                binding.tvMembersCount.text = "${group.members.size} участник${memberSuffix(group.members.size)}"
            }
        }

        // Show send/mic/vidnote based on text
        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val hasText = !s.isNullOrBlank()
                binding.btnSend.visibility      = if (hasText) View.VISIBLE else View.GONE
                binding.btnMic.visibility       = if (hasText) View.GONE   else View.VISIBLE
                binding.btnVideoNote.visibility = if (hasText) View.GONE   else View.VISIBLE
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendMessage(text)
                binding.etMessage.setText("")
            }
        }

        binding.btnAttach.setOnClickListener { showAttachmentMenu() }

        binding.btnVideoNote.setOnClickListener { openVideoNoteRecorder() }

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
                MotionEvent.ACTION_UP     -> { if (!isRecordingLocked) stopRecordingAndSend() }
                MotionEvent.ACTION_CANCEL -> { if (!isRecordingLocked) cancelRecording() }
            }
            true
        }

        binding.btnRecordDelete.setOnClickListener { cancelRecording() }
        binding.btnRecordSend.setOnClickListener   { stopRecordingAndSend() }
        binding.btnRecordPause.setOnClickListener  { togglePause() }

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnGroupInfo.setOnClickListener {
            val group = viewModel.group.value ?: run {
                Toast.makeText(requireContext(), "Загрузка данных группы…", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val sheet = GroupInfoBottomSheet.newInstance(group)
            sheet.onMemberAdded = { userId ->
                viewModel.addMember(userId) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "Участник добавлен", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Не удалось добавить участника", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            viewModel.group.observe(viewLifecycleOwner) { updatedGroup ->
                if (updatedGroup != null) sheet.updateGroup(updatedGroup)
            }
            sheet.show(parentFragmentManager, "group_info")
        }
    }

    private fun memberSuffix(n: Int): String = when {
        n % 100 in 11..19 -> "ов"
        n % 10 == 1        -> ""
        n % 10 in 2..4     -> "а"
        else               -> "ов"
    }

    private fun showAttachmentMenu() {
        val options = arrayOf("🖼️  Фото из галереи", "🎬  Видео из галереи", "📹  Записать видео", "📁  Файл")
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
            viewModel.sendMediaFile(file, "video/mp4", "video_note")
        }
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
        binding.inputPanel.visibility           = View.GONE
        binding.recordingPanel.visibility       = View.VISIBLE
        binding.layoutHoldHints.visibility      = View.VISIBLE
        binding.layoutLockedControls.visibility = View.GONE
        binding.tvRecordingTime.text = "0:00"
        recordingHandler.post(recordingTicker)
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
        viewModel.stopVoiceRecordingAndSend()
    }

    private fun cancelRecording() {
        recordingHandler.removeCallbacks(recordingTicker)
        isRecordingLocked = false
        isPaused = false
        binding.recordingPanel.visibility = View.GONE
        binding.inputPanel.visibility     = View.VISIBLE
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

    override fun onDestroyView() {
        recordingHandler.removeCallbacks(recordingTicker)
        super.onDestroyView()
        _binding = null
    }
}
