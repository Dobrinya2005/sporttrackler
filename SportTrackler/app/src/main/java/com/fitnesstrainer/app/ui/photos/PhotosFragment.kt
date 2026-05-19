package com.fitnesstrainer.app.ui.photos

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.fitnesstrainer.app.databinding.FragmentPhotosBinding

class PhotosFragment : Fragment() {

    private var _binding: FragmentPhotosBinding? = null
    private val binding get() = _binding!!
    private val args: PhotosFragmentArgs by navArgs()
    private val viewModel: PhotosViewModel by viewModels()
    private lateinit var adapter: PhotosAdapter

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.uploadPhoto(uri, null, null)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PhotosAdapter { photo -> showFullScreen(photo.photoUrl) }
        binding.rvPhotos.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvPhotos.adapter       = adapter

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.fabUpload.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImage.launch(intent)
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
                    binding.tvEmpty.text    = state.message
                    binding.tvEmptySub.text = ""
                    binding.btnRetry.visibility   = View.VISIBLE
                    binding.emptyState.visibility = View.VISIBLE
                }
            }
        }
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
