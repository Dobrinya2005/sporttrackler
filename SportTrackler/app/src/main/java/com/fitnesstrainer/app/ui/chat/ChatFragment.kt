package com.fitnesstrainer.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitnesstrainer.app.databinding.FragmentChatBinding

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val args: ChatFragmentArgs by navArgs()
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvContactName.text = args.contactName
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvMessages.layoutManager = layoutManager

        viewModel.myUserId.observe(viewLifecycleOwner) { myId ->
            if (myId != -1) {
                adapter = ChatAdapter(myId)
                binding.rvMessages.adapter = adapter
            }
        }

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            if (::adapter.isInitialized) {
                adapter.submitList(messages) {
                    if (messages.isNotEmpty()) {
                        binding.rvMessages.scrollToPosition(messages.size - 1)
                    }
                }
            }
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendMessage(text)
                binding.etMessage.setText("")
            }
        }

        viewModel.init(args.contactId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
