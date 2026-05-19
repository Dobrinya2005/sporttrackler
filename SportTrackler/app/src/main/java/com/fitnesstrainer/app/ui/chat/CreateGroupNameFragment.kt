package com.fitnesstrainer.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.model.CreateGroupRequest
import com.fitnesstrainer.app.databinding.FragmentCreateGroupNameBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateGroupNameFragment : Fragment() {

    private var _binding: FragmentCreateGroupNameBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateGroupNameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val memberIds = arguments?.getIntArray("memberIds")?.toList() ?: emptyList()

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnCreate.setOnClickListener {
            val name = binding.etGroupName.text?.toString()?.trim() ?: ""
            if (name.isBlank()) {
                Toast.makeText(requireContext(), "Введите название группы", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            createGroup(name, memberIds)
        }
    }

    private fun createGroup(name: String, memberIds: List<Int>) {
        binding.btnCreate.isEnabled = false
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = App.instance.apiService.createGroup(CreateGroupRequest(name, memberIds))
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful) {
                        val group = resp.body()!!
                        Toast.makeText(requireContext(), "Группа создана!", Toast.LENGTH_SHORT).show()
                        // Открыть чат группы
                        findNavController().navigate(
                            R.id.groupChatFragment,
                            bundleOf(
                                "groupId"   to group.groupId,
                                "groupName" to group.name
                            )
                        )
                    } else {
                        Toast.makeText(requireContext(), "Ошибка создания группы", Toast.LENGTH_SHORT).show()
                        binding.btnCreate.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnCreate.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
