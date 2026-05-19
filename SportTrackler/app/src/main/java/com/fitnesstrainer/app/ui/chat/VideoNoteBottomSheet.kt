package com.fitnesstrainer.app.ui.chat

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Outline
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.databinding.FragmentVideoNoteBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File

class VideoNoteBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentVideoNoteBinding? = null
    private val binding get() = _binding!!

    var onVideoReady: ((File) -> Unit)? = null

    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var outputFile: File? = null
    private var isRecording = false

    private var useFrontCamera = true

    private var recordingSeconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            recordingSeconds++
            val m = recordingSeconds / 60
            val s = recordingSeconds % 60
            binding.tvTimer.text = "%d:%02d".format(m, s)
            if (recordingSeconds >= 60) stopAndSend()
            else handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVideoNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = true

        binding.previewView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
        binding.previewView.clipToOutline = true

        startCamera()

        binding.btnFlipCamera.setOnClickListener {
            if (!isRecording) flipCamera()
        }

        binding.btnRecord.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> startRecording()
                MotionEvent.ACTION_UP   -> stopAndSend()
                MotionEvent.ACTION_CANCEL -> cancelRecording()
            }
            true
        }

        binding.btnCancel.setOnClickListener {
            cancelRecording()
            dismiss()
        }
    }

    private fun buildCameraSelector(): CameraSelector {
        val hasFront = cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) == true
        val hasBack  = cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) == true
        return when {
            useFrontCamera && hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA
            hasBack -> CameraSelector.DEFAULT_BACK_CAMERA
            hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA
            else -> CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(requireContext())
        future.addListener({
            cameraProvider = future.get()
            bindCamera()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCamera() {
        val provider = cameraProvider ?: return
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.previewView.surfaceProvider)
        }

        val selector = buildCameraSelector()

        // Правильный способ получить CameraInfo для выбранного селектора
        val cameraInfo = try {
            selector.filter(provider.availableCameraInfos).firstOrNull()
        } catch (_: Exception) { null } ?: provider.availableCameraInfos.firstOrNull()

        val supportedQualities = cameraInfo?.let {
            QualitySelector.getSupportedQualities(it)
        } ?: emptyList()

        // SD/LOWEST предпочитаем — они обычно используют AVC (H.264), не HEVC.
        // Если ни одно не поддерживается — берём что есть (HD/HIGHEST).
        val preferAvc = listOf(Quality.SD, Quality.LOWEST, Quality.HD, Quality.HIGHEST)
            .filter { it in supportedQualities }
            .ifEmpty { supportedQualities.ifEmpty { listOf(Quality.LOWEST) } }

        android.util.Log.d("VideoNote", "supported=$supportedQualities chosen=${preferAvc.firstOrNull()}")

        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.fromOrderedList(
                    preferAvc,
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.LOWEST)
                )
            )
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        provider.unbindAll()
        try {
            provider.bindToLifecycle(viewLifecycleOwner, selector, preview, videoCapture)
        } catch (_: Exception) {
            try {
                provider.bindToLifecycle(
                    viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, videoCapture
                )
            } catch (_: Exception) {}
        }
    }

    private fun flipCamera() {
        useFrontCamera = !useFrontCamera
        binding.btnFlipCamera.animate().rotationBy(180f).setDuration(300).start()
        bindCamera()
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        if (isRecording) return
        val vc = videoCapture ?: run {
            android.widget.Toast.makeText(requireContext(), "Камера не готова", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val file = File(requireContext().cacheDir, "vidnote_${System.currentTimeMillis()}.mp4")
        outputFile = file
        try {
            recording = vc.output
                .prepareRecording(requireContext(), FileOutputOptions.Builder(file).build())
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(requireContext())) { event ->
                    if (event is VideoRecordEvent.Finalize) {
                        if (!event.hasError()) {
                            outputFile?.let { onVideoReady?.invoke(it) }
                        } else {
                            outputFile?.delete()
                            outputFile = null
                        }
                    }
                }
        } catch (e: Exception) {
            file.delete()
            outputFile = null
            android.widget.Toast.makeText(requireContext(), "Ошибка записи видео", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        isRecording = true
        recordingSeconds = 0
        binding.tvTimer.visibility = View.VISIBLE
        binding.tvTimer.text = "0:00"
        binding.tvHint.visibility = View.GONE
        binding.btnFlipCamera.visibility = View.GONE
        binding.btnRecord.setBackgroundResource(R.drawable.bg_record_btn_active)
        pulseRing()
        handler.postDelayed(ticker, 1000)
    }

    private fun stopAndSend() {
        if (!isRecording) return
        isRecording = false
        handler.removeCallbacks(ticker)
        recording?.stop()
        recording = null
        dismiss()
    }

    private fun cancelRecording() {
        if (!isRecording) return
        isRecording = false
        handler.removeCallbacks(ticker)
        recording?.stop()
        recording = null
        outputFile?.delete()
        outputFile = null
    }

    private fun pulseRing() {
        ObjectAnimator.ofFloat(binding.btnRecordRing, "scaleX", 1f, 1.15f, 1f).apply {
            duration = 600
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
        ObjectAnimator.ofFloat(binding.btnRecordRing, "scaleY", 1f, 1.15f, 1f).apply {
            duration = 600
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    override fun onDestroyView() {
        handler.removeCallbacks(ticker)
        cameraProvider?.unbindAll()
        super.onDestroyView()
        _binding = null
    }
}
