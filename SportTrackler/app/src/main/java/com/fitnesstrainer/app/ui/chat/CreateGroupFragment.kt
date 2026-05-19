package com.fitnesstrainer.app.ui.chat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.model.ConversationPreview
import com.fitnesstrainer.app.databinding.FragmentCreateGroupBinding
import com.fitnesstrainer.app.databinding.ItemContactSelectBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateGroupFragment : Fragment() {

    private var _binding: FragmentCreateGroupBinding? = null
    private val binding get() = _binding!!

    private val selected = mutableMapOf<Int, ConversationPreview>()
    private lateinit var contactAdapter: ContactSelectAdapter
    private var allContacts: List<ConversationPreview> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        contactAdapter = ContactSelectAdapter(
            isSelected = { selected.containsKey(it.contactId) },
            onToggle   = { contact -> toggleSelection(contact) }
        )
        binding.rvContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvContacts.adapter       = contactAdapter

        loadContacts()

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterContacts(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnNext.setOnClickListener {
            if (selected.isEmpty()) return@setOnClickListener
            val ids   = selected.keys.toIntArray()
            val names = selected.values.map { it.contactName }.toTypedArray()
            findNavController().navigate(
                R.id.createGroupNameFragment,
                bundleOf("memberIds" to ids, "memberNames" to names)
            )
        }
    }

    private fun loadContacts() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = App.instance.apiService.getConversations()
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful) {
                        allContacts = resp.body() ?: emptyList()
                        contactAdapter.submitList(allContacts)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun filterContacts(query: String) {
        val filtered = if (query.isBlank()) allContacts
        else allContacts.filter { it.contactName.contains(query, ignoreCase = true) }
        contactAdapter.submitList(filtered)
    }

    private fun toggleSelection(contact: ConversationPreview) {
        if (selected.containsKey(contact.contactId)) {
            selected.remove(contact.contactId)
        } else {
            selected[contact.contactId] = contact
        }
        updateChips()
        contactAdapter.notifyDataSetChanged()
        binding.btnNext.text = if (selected.isEmpty()) "Далее" else "Далее (${selected.size})"
    }

    private fun updateChips() {
        binding.scrollSelected.visibility  = if (selected.isEmpty()) View.GONE else View.VISIBLE
        binding.chipsContainer.removeAllViews()
        selected.values.forEach { contact ->
            val chip = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_group_chip, binding.chipsContainer, false)
            chip.findViewById<TextView>(R.id.tv_chip_name).text = contact.contactName.split(" ").first()
            chip.setOnClickListener { toggleSelection(contact) }
            binding.chipsContainer.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ContactSelectAdapter(
    private val isSelected: (ConversationPreview) -> Boolean,
    private val onToggle:   (ConversationPreview) -> Unit
) : ListAdapter<ConversationPreview, ContactSelectAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemContactSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position))

    inner class VH(private val b: ItemContactSelectBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(contact: ConversationPreview) {
            b.tvName.text = contact.contactName
            if (!contact.contactAvatar.isNullOrBlank()) {
                Glide.with(b.root).load(contact.contactAvatar).circleCrop().into(b.ivAvatar)
            } else {
                b.ivAvatar.setImageResource(R.drawable.ic_person)
            }
            b.ivCheck.visibility = if (isSelected(contact)) View.VISIBLE else View.GONE
            b.root.setOnClickListener { onToggle(contact) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ConversationPreview>() {
            override fun areItemsTheSame(a: ConversationPreview, b: ConversationPreview) = a.contactId == b.contactId
            override fun areContentsTheSame(a: ConversationPreview, b: ConversationPreview) = a == b
        }
    }
}
