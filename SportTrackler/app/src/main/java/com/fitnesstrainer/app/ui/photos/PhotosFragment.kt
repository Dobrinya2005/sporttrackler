package com.fitnesstrainer.app.ui.photos

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
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

        adapter = PhotosAdapter { /* full-screen preview placeholder */ }
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

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PhotosState.Loading -> binding.progressBar.visibility = View.VISIBLE
                is PhotosState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    adapter.submitList(state.photos)
                    binding.tvEmpty.visibility =
                        if (state.photos.isEmpty()) View.VISIBLE else View.GONE
                    binding.fabUpload.visibility =
                        if (viewModel.isOwnData) View.VISIBLE else View.GONE
                }
                is PhotosState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
