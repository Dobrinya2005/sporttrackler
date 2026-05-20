package com.fitnesstrainer.app.ui.chat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.model.ConversationPreview
import com.fitnesstrainer.app.data.model.GroupDto
import com.fitnesstrainer.app.data.model.GroupMemberDto
import com.fitnesstrainer.app.databinding.BottomSheetGroupInfoBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GroupInfoBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetGroupInfoBinding? = null
    private val binding get() = _binding!!

    var onMemberAdded: ((userId: Int) -> Unit)? = null

    private lateinit var memberAdapter: GroupMemberAdapter
    private var group: GroupDto? = null

    companion object {
        private const val ARG_GROUP_ID   = "groupId"
        private const val ARG_GROUP_NAME = "groupName"

        fun newInstance(group: GroupDto): GroupInfoBottomSheet {
            val sheet = GroupInfoBottomSheet()
            sheet.arguments = Bundle().apply {
                putInt(ARG_GROUP_ID, group.groupId)
                putString(ARG_GROUP_NAME, group.name)
            }
            sheet.group = group
            return sheet
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetGroupInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        memberAdapter = GroupMemberAdapter()
        binding.rvMembers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMembers.adapter = memberAdapter

        group?.let { bindGroup(it) }

        binding.btnAddMember.setOnClickListener { showAddMemberDialog() }
    }

    fun updateGroup(g: GroupDto) {
        group = g
        if (_binding != null) bindGroup(g)
    }

    private fun bindGroup(g: GroupDto) {
        binding.tvSheetGroupName.text = g.name
        val count = g.members.size
        binding.tvSheetMembersCount.text = "$count участник${memberSuffix(count)}"
        if (!g.avatarUrl.isNullOrBlank()) {
            Glide.with(this).load(g.avatarUrl).circleCrop().into(binding.ivGroupAvatarSheet)
        }
        memberAdapter.submitList(g.members)
    }

    private fun memberSuffix(n: Int): String = when {
        n % 100 in 11..19 -> "ов"
        n % 10 == 1        -> ""
        n % 10 in 2..4     -> "а"
        else               -> "ов"
    }

    private fun showAddMemberDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_member, null)
        val etSearch   = dialogView.findViewById<EditText>(R.id.et_search)
        val rvContacts = dialogView.findViewById<RecyclerView>(R.id.rv_contacts)

        val existingIds = group?.members?.map { it.userId }?.toSet() ?: emptySet()

        val adapter = ContactSelectAdapter(
            isSelected = { false },
            onToggle   = { contact ->
                dismiss()
                onMemberAdded?.invoke(contact.contactId)
            }
        )
        rvContacts.layoutManager = LinearLayoutManager(requireContext())
        rvContacts.adapter = adapter

        var allContacts: List<ConversationPreview> = emptyList()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = App.instance.apiService.getConversations()
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful) {
                        allContacts = (resp.body() ?: emptyList())
                            .filter { it.contactId !in existingIds }
                        adapter.submitList(allContacts)
                    }
                }
            } catch (_: Exception) {}
        }

        etSearch?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString() ?: ""
                adapter.submitList(if (q.isBlank()) allContacts
                else allContacts.filter { it.contactName.contains(q, ignoreCase = true) })
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        dialogView.findViewById<TextView>(R.id.btn_cancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class GroupMemberAdapter : ListAdapter<GroupMemberDto, GroupMemberAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_member_row, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_member_avatar)
        private val tvName: TextView    = itemView.findViewById(R.id.tv_member_name)
        private val tvAdmin: TextView   = itemView.findViewById(R.id.tv_admin_badge)

        fun bind(member: GroupMemberDto) {
            tvName.text = member.name
            tvAdmin.visibility = if (member.isAdmin) View.VISIBLE else View.GONE
            if (!member.avatar.isNullOrBlank()) {
                Glide.with(itemView).load(member.avatar).circleCrop()
                    .placeholder(R.drawable.ic_person).into(ivAvatar)
            } else {
                ivAvatar.setImageResource(R.drawable.ic_person)
            }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<GroupMemberDto>() {
            override fun areItemsTheSame(a: GroupMemberDto, b: GroupMemberDto) = a.userId == b.userId
            override fun areContentsTheSame(a: GroupMemberDto, b: GroupMemberDto) = a == b
        }
    }
}
