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

        // Круговая обрезка превью через ViewOutlineProvider (надёжнее чем clipToOutline в XML)
        binding.previewView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
        binding.previewView.clipToOutline = true

        startCamera()

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

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(requireContext())
        future.addListener({
            cameraProvider = future.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            cameraProvider?.unbindAll()
            val selector = try {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } catch (_: Exception) {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            try {
                cameraProvider?.bindToLifecycle(viewLifecycleOwner, selector, preview, videoCapture)
            } catch (_: Exception) {
                try {
                    cameraProvider?.bindToLifecycle(
                        viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, videoCapture
                    )
                } catch (_: Exception) {}
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        if (isRecording) return
        val file = File(requireContext().cacheDir, "vidnote_${System.currentTimeMillis()}.mp4")
        outputFile = file
        recording = videoCapture?.output
            ?.prepareRecording(requireContext(), FileOutputOptions.Builder(file).build())
            ?.withAudioEnabled()
            ?.start(ContextCompat.getMainExecutor(requireContext())) { event ->
                if (event is VideoRecordEvent.Finalize && !event.hasError()) {
                    outputFile?.let { onVideoReady?.invoke(it) }
                }
            }
        isRecording = true
        recordingSeconds = 0
        binding.tvTimer.visibility = View.VISIBLE
        binding.tvTimer.text = "0:00"
        binding.tvHint.visibility = View.GONE
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
