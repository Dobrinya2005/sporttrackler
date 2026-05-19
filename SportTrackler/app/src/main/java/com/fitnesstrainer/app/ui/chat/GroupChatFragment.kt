package com.fitnesstrainer.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.databinding.FragmentGroupChatBinding
import kotlinx.coroutines.runBlocking

class GroupChatFragment : Fragment() {

    private var _binding: FragmentGroupChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GroupChatViewModel by viewModels()
    private lateinit var adapter: GroupChatAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGroupChatBinding.inflate(inflater, container, false)
        return binding.root
    }

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

        viewModel.messages.observe(viewLifecycleOwner) { msgs ->
            adapter.submitList(msgs.toList())
            if (msgs.isNotEmpty()) binding.rvMessages.scrollToPosition(msgs.size - 1)
        }

        binding.btnSend.setOnClickListener { sendMessage() }
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
        }

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnGroupInfo.setOnClickListener {
            // TODO: показать список участников группы
        }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text?.toString()?.trim() ?: return
        if (text.isBlank()) return
        binding.etMessage.setText("")
        viewModel.sendMessage(text)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
