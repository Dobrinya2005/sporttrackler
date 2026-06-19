package com.fitnesstrainer.app.ui.photos

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.fitnesstrainer.app.data.model.ProgressPhoto
import com.fitnesstrainer.app.databinding.DialogComparePhotosBinding
import com.fitnesstrainer.app.databinding.FragmentPhotosBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PhotosFragment : Fragment() {

    private var _binding: FragmentPhotosBinding? = null
    private val binding get() = _binding!!
    private val args: PhotosFragmentArgs by navArgs()
    private val viewModel: PhotosViewModel by viewModels()
    private lateinit var adapter: PhotosAdapter

    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@registerForActivityResult
        viewModel.uploadPhoto(uri, null, null)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PhotosAdapter(
            onClick = { photo ->
                if (!adapter.isSelecting) showFullScreen(photo.photoUrl)
            },
            onSelectionChanged = { selected ->
                val count = selected.size
                binding.compareBar.visibility = if (count >= 1) View.VISIBLE else View.GONE
                binding.tvSelectedCount.text  = "$count фото выбрано"
                binding.btnDelete.visibility  = if (count == 1) View.VISIBLE else View.GONE
                binding.btnCompare.visibility = if (count == 2) View.VISIBLE else View.GONE
                binding.fabUpload.visibility  = if (adapter.isSelecting) View.GONE else
                    if (viewModel.isOwnData) View.VISIBLE else View.GONE
            }
        )
        binding.rvPhotos.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvPhotos.adapter       = adapter

        binding.btnCancelCompare.setOnClickListener {
            adapter.clearSelection()
        }
        binding.btnCompare.setOnClickListener {
            val sel = adapter.getSelected()
            if (sel.size == 2) showCompareDialog(sel[0], sel[1])
        }
        binding.btnDelete.setOnClickListener {
            val sel = adapter.getSelected()
            if (sel.size == 1) confirmDelete(sel[0])
        }

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.swipeRefresh.setColorSchemeResources(
            com.fitnesstrainer.app.R.color.accent_violet,
            com.fitnesstrainer.app.R.color.accent_indigo
        )
        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }

        binding.fabUpload.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        viewModel.init(args.clientId)

        viewModel.uploading.observe(viewLifecycleOwner) { uploading ->
            binding.progressBar.visibility = if (uploading) View.VISIBLE else View.GONE
            binding.fabUpload.isEnabled    = !uploading
        }

        binding.btnRetry.setOnClickListener { viewModel.load() }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PhotosState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.emptyState.visibility  = View.GONE
                }
                is PhotosState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    adapter.submitList(state.photos)
                    binding.fabUpload.visibility = if (viewModel.isOwnData) View.VISIBLE else View.GONE
                    if (state.photos.isEmpty()) {
                        binding.tvEmpty.text    = "Фото пока нет"
                        binding.tvEmptySub.text = "Нажмите + чтобы загрузить фото"
                        binding.btnRetry.visibility   = View.GONE
                        binding.emptyState.visibility = View.VISIBLE
                    } else {
                        binding.emptyState.visibility = View.GONE
                    }
                }
                is PhotosState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    binding.tvEmpty.text    = state.message
                    binding.tvEmptySub.text = ""
                    binding.btnRetry.visibility   = View.VISIBLE
                    binding.emptyState.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun confirmDelete(photo: ProgressPhoto) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить фото?")
            .setMessage("Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deletePhoto(photo.photoId) { adapter.clearSelection() }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showCompareDialog(p1: ProgressPhoto, p2: ProgressPhoto) {
        val sorted = listOf(p1, p2).sortedBy { it.photoDate }
        val before = sorted[0]
        val after  = sorted[1]

        val db = DialogComparePhotosBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        dialog.setContentView(db.root)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                (resources.displayMetrics.widthPixels * 0.95).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        Glide.with(this).load(before.photoUrl).centerCrop().into(db.ivBefore)
        Glide.with(this).load(after.photoUrl).centerCrop().into(db.ivAfter)

        fun fmt(d: String) = try {
            LocalDate.parse(d).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        } catch (_: Exception) { d.take(10) }
        db.tvDateBefore.text = fmt(before.photoDate)
        db.tvDateAfter.text  = fmt(after.photoDate)

        db.btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showFullScreen(url: String) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(requireContext()).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener { dialog.dismiss() }
        }
        dialog.setContentView(imageView)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        Glide.with(this).load(url).into(imageView)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
