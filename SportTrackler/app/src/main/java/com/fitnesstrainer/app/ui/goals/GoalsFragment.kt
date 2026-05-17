package com.fitnesstrainer.app.ui.goals

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitnesstrainer.app.data.local.db.GoalType
import com.fitnesstrainer.app.databinding.DialogAddGoalBinding
import com.fitnesstrainer.app.databinding.FragmentGoalsBinding

class GoalsFragment : Fragment() {

    private var _b: FragmentGoalsBinding? = null
    private val b get() = _b!!
    private val viewModel: GoalsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentGoalsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = GoalsAdapter(
            onDelete = { type -> viewModel.deleteGoal(type) }
        )
        b.rvGoals.layoutManager = LinearLayoutManager(requireContext())
        b.rvGoals.adapter       = adapter

        b.btnBack.setOnClickListener { findNavController().popBackStack() }
        b.fabAdd.setOnClickListener  { showAddDialog() }

        viewModel.goals.observe(viewLifecycleOwner) { goals ->
            adapter.submitList(goals)
            b.emptyState.visibility = if (goals.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.load()
    }

    private fun showAddDialog() {
        val dialogBinding = DialogAddGoalBinding.inflate(layoutInflater)
        val types = GoalType.values()
        val labels = types.map { labelFor(it) }
        dialogBinding.spinnerType.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, labels
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        AlertDialog.Builder(requireContext())
            .setTitle("Новая цель")
            .setView(dialogBinding.root)
            .setPositiveButton("Сохранить") { _, _ ->
                val type  = types[dialogBinding.spinnerType.selectedItemPosition]
                val value = dialogBinding.etValue.text.toString().toDoubleOrNull() ?: return@setPositiveButton
                viewModel.setGoal(type, value)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun labelFor(type: GoalType) = when (type) {
        GoalType.WEIGHT   -> "Вес (кг)"
        GoalType.BODY_FAT -> "% жира"
        GoalType.CHEST    -> "Грудь (см)"
        GoalType.WAIST    -> "Талия (см)"
        GoalType.HIPS     -> "Бёдра (см)"
        GoalType.BICEP    -> "Бицепс (см)"
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
