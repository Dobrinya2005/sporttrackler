package com.fitnesstrainer.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.databinding.FragmentChatsBinding

class ChatsFragment : Fragment() {

    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatsViewModel by viewModels()
    private lateinit var adapter: ChatsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ChatsAdapter(
            onTrainerClick = { contactId, contactName ->
                val action = ChatsFragmentDirections.actionChatsToChat(
                    contactId = contactId,
                    contactName = contactName
                )
                findNavController().navigate(action)
            },
            onGroupClick = { groupId, groupName ->
                val action = ChatsFragmentDirections.actionChatsToGroupChat(
                    groupId = groupId,
                    groupName = groupName
                )
                findNavController().navigate(action)
            }
        )

        binding.rvChats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChats.adapter = adapter

        binding.fabCreateGroup.setOnClickListener {
            findNavController().navigate(R.id.action_chats_to_createGroup)
        }

        viewModel.items.observe(viewLifecycleOwner) { items ->
            binding.progressBar.visibility = View.GONE
            adapter.submitList(items)
            binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.load()
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
