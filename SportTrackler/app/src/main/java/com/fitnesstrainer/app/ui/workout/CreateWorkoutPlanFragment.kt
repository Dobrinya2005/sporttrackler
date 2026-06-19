package com.fitnesstrainer.app.ui.workout

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.fitnesstrainer.app.data.model.ExerciseRequest
import com.fitnesstrainer.app.data.model.WorkoutDayRequest
import com.fitnesstrainer.app.databinding.FragmentCreateWorkoutPlanBinding
import com.fitnesstrainer.app.databinding.ItemEditDayBinding
import com.fitnesstrainer.app.databinding.ItemEditExerciseBinding
import java.util.Calendar

class CreateWorkoutPlanFragment : Fragment() {

    private var _binding: FragmentCreateWorkoutPlanBinding? = null
    private val binding get() = _binding!!
    private val args: CreateWorkoutPlanFragmentArgs by navArgs()
    private val viewModel: CreateWorkoutPlanViewModel by viewModels()

    private val dayBindings = mutableListOf<ItemEditDayBinding>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateWorkoutPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnAddDay.setOnClickListener { addDay() }
        binding.btnSave.setOnClickListener { save() }

        binding.etStartDate.setOnClickListener { pickDate(true) }
        binding.etEndDate.setOnClickListener { pickDate(false) }

        addDay()

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CreatePlanState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSave.isEnabled = false
                }
                is CreatePlanState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "План успешно создан!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                is CreatePlanState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
                is CreatePlanState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                }
            }
        }
    }

    private fun addDay() {
        val dayNumber = dayBindings.size + 1
        val dayBinding = ItemEditDayBinding.inflate(layoutInflater, binding.daysContainer, false)
        dayBinding.tvDayNumber.text = "День $dayNumber"
        dayBinding.btnAddExercise.setOnClickListener { addExercise(dayBinding) }
        dayBinding.btnRemoveDay.setOnClickListener {
            binding.daysContainer.removeView(dayBinding.root)
            dayBindings.remove(dayBinding)
            renumberDays()
        }
        addExercise(dayBinding)
        binding.daysContainer.addView(dayBinding.root)
        dayBindings.add(dayBinding)
    }

    private fun addExercise(dayBinding: ItemEditDayBinding) {
        val exBinding = ItemEditExerciseBinding.inflate(
            layoutInflater, dayBinding.exercisesContainer, false
        )
        exBinding.root.tag = exBinding
        exBinding.btnRemoveExercise.setOnClickListener {
            dayBinding.exercisesContainer.removeView(exBinding.root)
        }
        dayBinding.exercisesContainer.addView(exBinding.root)
    }

    private fun renumberDays() {
        dayBindings.forEachIndexed { index, b ->
            b.tvDayNumber.text = "День ${index + 1}"
        }
    }

    private fun pickDate(isStart: Boolean) {
        val today = Calendar.getInstance()
        val dialog = DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                val formatted = "%04d-%02d-%02d".format(y, m + 1, d)
                val display   = "%02d.%02d.%04d".format(d, m + 1, y)
                if (isStart) {
                    binding.etStartDate.setText(display)
                    binding.etStartDate.tag = formatted
                    // Если дата окончания раньше новой даты начала — сбрасываем её
                    val endTag = binding.etEndDate.tag as? String
                    if (endTag != null && endTag < formatted) {
                        binding.etEndDate.setText("")
                        binding.etEndDate.tag = null
                    }
                } else {
                    binding.etEndDate.setText(display)
                    binding.etEndDate.tag = formatted
                }
            },
            today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)
        )
        // Минимальная дата: сегодня для начала, дата начала (или сегодня) для конца
        if (isStart) {
            dialog.datePicker.minDate = today.timeInMillis
        } else {
            val startTag = binding.etStartDate.tag as? String
            if (startTag != null) {
                val startCal = Calendar.getInstance().apply {
                    val parts = startTag.split("-")
                    set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                }
                dialog.datePicker.minDate = maxOf(today.timeInMillis, startCal.timeInMillis)
            } else {
                dialog.datePicker.minDate = today.timeInMillis
            }
        }
        dialog.show()
    }

    private fun save() {
        val title       = binding.etTitle.text?.toString() ?: ""
        val description = binding.etDescription.text?.toString()
        val startDate   = binding.etStartDate.tag as? String
        val endDate     = binding.etEndDate.tag as? String

        val days = dayBindings.mapIndexed { dayIndex, dayB ->
            val dayName   = dayB.etDayName.text?.toString()?.trim()?.ifBlank { null }
            val exercises = mutableListOf<ExerciseRequest>()
            for (i in 0 until dayB.exercisesContainer.childCount) {
                val exView = dayB.exercisesContainer.getChildAt(i)
                val exB    = exView.tag as? ItemEditExerciseBinding ?: continue
                val name   = exB.etExerciseName.text?.toString()?.trim() ?: continue
                if (name.isBlank()) continue
                exercises.add(
                    ExerciseRequest(
                        orderIndex   = i + 1,
                        name         = name,
                        muscleGroup  = exB.etMuscleGroup.text?.toString()?.trim()?.ifBlank { null },
                        sets         = exB.etSets.text?.toString()?.toIntOrNull(),
                        reps         = exB.etReps.text?.toString()?.trim()?.ifBlank { null },
                        weightKg     = exB.etWeight.text?.toString()?.toDoubleOrNull(),
                        restSeconds  = exB.etRest.text?.toString()?.toIntOrNull(),
                        videoUrl     = null,
                        notes        = exB.etExerciseNotes.text?.toString()?.trim()?.ifBlank { null }
                    )
                )
            }
            WorkoutDayRequest(
                dayNumber  = dayIndex + 1,
                dayName    = dayName,
                notes      = null,
                exercises  = exercises
            )
        }

        viewModel.createPlan(
            clientId    = args.clientId,
            title       = title,
            description = description,
            startDate   = startDate,
            endDate     = endDate,
            days        = days
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
