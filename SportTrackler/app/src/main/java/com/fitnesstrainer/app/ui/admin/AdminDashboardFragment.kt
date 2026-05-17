package com.fitnesstrainer.app.ui.admin

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.model.AdminTrainerItem
import com.fitnesstrainer.app.databinding.DialogCreateTrainerBinding
import com.fitnesstrainer.app.databinding.FragmentAdminDashboardBinding

class AdminDashboardFragment : Fragment() {

    private var _b: FragmentAdminDashboardBinding? = null
    private val b get() = _b!!
    private val vm: AdminViewModel by viewModels()
    private lateinit var adapter: AdminTrainerAdapter
    private var allTrainers: List<AdminTrainerItem> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminTrainerAdapter(
            onReviews = { trainer ->
                findNavController().navigate(
                    R.id.action_adminDashboard_to_adminReviews,
                    bundleOf("trainerId" to trainer.userId, "trainerName" to "${trainer.firstName} ${trainer.lastName}")
                )
            },
            onBlock = { trainer ->
                val action = if (trainer.isActive) "заблокировать" else "разблокировать"
                AlertDialog.Builder(requireContext())
                    .setTitle("Подтверждение")
                    .setMessage("${trainer.firstName} ${trainer.lastName}: $action?")
                    .setPositiveButton("Да") { _, _ -> vm.toggleBlock(trainer.userId) }
                    .setNegativeButton("Отмена", null)
                    .show()
            },
            onDelete = { trainer ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Удалить тренера")
                    .setMessage("Удалить ${trainer.firstName} ${trainer.lastName}? Это действие необратимо.")
                    .setPositiveButton("Удалить") { _, _ -> vm.deleteTrainer(trainer.userId) }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
        )

        b.rvTrainers.layoutManager = LinearLayoutManager(requireContext())
        b.rvTrainers.adapter = adapter

        b.btnAddTrainer.setOnClickListener { showCreateDialog() }
        b.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboard_to_adminSettings)
        }

        b.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = filterTrainers(s?.toString() ?: "")
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        vm.trainers.observe(viewLifecycleOwner) { trainers ->
            allTrainers = trainers
            filterTrainers(b.etSearch.text?.toString() ?: "")
        }

        vm.loading.observe(viewLifecycleOwner) { loading ->
            b.progress.visibility = if (loading) View.VISIBLE else View.GONE
        }

        vm.error.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                vm.clearMessages()
            }
        }

        vm.success.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                vm.clearMessages()
            }
        }

        vm.loadTrainers()
    }

    private fun filterTrainers(query: String) {
        val filtered = if (query.isBlank()) allTrainers
        else allTrainers.filter {
            it.firstName.contains(query, true) ||
            it.lastName.contains(query, true) ||
            it.email.contains(query, true) ||
            it.trainerCode.contains(query, true)
        }
        adapter.submitList(filtered)
    }

    private fun showCreateDialog() {
        val dialogBinding = DialogCreateTrainerBinding.inflate(layoutInflater)
        AlertDialog.Builder(requireContext())
            .setTitle("Новый тренер")
            .setView(dialogBinding.root)
            .setPositiveButton("Создать") { _, _ ->
                val firstName = dialogBinding.etFirstName.text?.toString()?.trim() ?: ""
                val lastName  = dialogBinding.etLastName.text?.toString()?.trim() ?: ""
                val email     = dialogBinding.etEmail.text?.toString()?.trim() ?: ""
                val password  = dialogBinding.etPassword.text?.toString()?.trim() ?: ""
                if (firstName.isBlank() || email.isBlank() || password.length < 6) {
                    Toast.makeText(requireContext(), "Заполните все поля (пароль ≥ 6 символов)", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                vm.createTrainer(firstName, lastName, email, password)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
