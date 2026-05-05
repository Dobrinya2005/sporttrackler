package com.fitnesstrainer.app.ui.workout

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fitnesstrainer.app.data.model.Exercise
import com.fitnesstrainer.app.data.model.WorkoutDay
import com.fitnesstrainer.app.data.model.WorkoutPlan
import com.fitnesstrainer.app.databinding.ItemExerciseBinding
import com.fitnesstrainer.app.databinding.ItemWorkoutDayBinding
import com.fitnesstrainer.app.databinding.ItemWorkoutPlanHeaderBinding

private const val TYPE_PLAN_HEADER = 0
private const val TYPE_DAY_HEADER  = 1
private const val TYPE_EXERCISE    = 2

sealed class WorkoutItem {
    data class PlanHeader(val plan: WorkoutPlan) : WorkoutItem()
    data class DayHeader(val day: WorkoutDay)   : WorkoutItem()
    data class ExerciseItem(val ex: Exercise)   : WorkoutItem()
}

class WorkoutPlanAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<WorkoutItem>()

    fun submitPlans(plans: List<WorkoutPlan>) {
        items.clear()
        for (plan in plans) {
            items.add(WorkoutItem.PlanHeader(plan))
            for (day in plan.days) {
                items.add(WorkoutItem.DayHeader(day))
                day.exercises.forEach { items.add(WorkoutItem.ExerciseItem(it)) }
            }
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = when (items[position]) {
        is WorkoutItem.PlanHeader   -> TYPE_PLAN_HEADER
        is WorkoutItem.DayHeader    -> TYPE_DAY_HEADER
        is WorkoutItem.ExerciseItem -> TYPE_EXERCISE
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_PLAN_HEADER -> PlanHeaderVH(
                ItemWorkoutPlanHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            TYPE_DAY_HEADER  -> DayHeaderVH(
                ItemWorkoutDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            else -> ExerciseVH(
                ItemExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is WorkoutItem.PlanHeader   -> (holder as PlanHeaderVH).bind(item.plan)
            is WorkoutItem.DayHeader    -> (holder as DayHeaderVH).bind(item.day)
            is WorkoutItem.ExerciseItem -> (holder as ExerciseVH).bind(item.ex)
        }
    }

    class PlanHeaderVH(private val b: ItemWorkoutPlanHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(plan: WorkoutPlan) {
            b.tvPlanTitle.text = plan.title
            b.tvPlanDesc.text  = plan.description ?: ""
            val dates = buildString {
                plan.startDate?.let { append(it.take(10)) }
                plan.endDate?.let   { append(" — ${it.take(10)}") }
            }
            b.tvDates.text = dates
        }
    }

    class DayHeaderVH(private val b: ItemWorkoutDayBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(day: WorkoutDay) {
            b.tvDayName.text = day.dayName ?: "День ${day.dayNumber}"
        }
    }

    class ExerciseVH(private val b: ItemExerciseBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(ex: Exercise) {
            b.tvExerciseName.text  = ex.name
            b.tvMuscleGroup.text   = ex.muscleGroup ?: ""
            val sets = buildString {
                ex.sets?.let { append("${it} подх.") }
                ex.reps?.let { append(" × $it") }
                ex.weightKg?.let { append("  %.1f кг".format(it)) }
                ex.restSeconds?.let { append("  Отдых: ${it}с") }
            }
            b.tvSets.text  = sets
            b.tvNotes.text = ex.notes ?: ""
        }
    }
}
