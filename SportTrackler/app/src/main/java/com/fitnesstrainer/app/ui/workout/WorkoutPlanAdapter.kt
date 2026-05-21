package com.fitnesstrainer.app.ui.workout

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.fitnesstrainer.app.R
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
    data class ExerciseItem(val ex: Exercise, val planId: Int, val dayId: Int) : WorkoutItem()
}

class WorkoutPlanAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<WorkoutItem>()
    private var prefs: android.content.SharedPreferences? = null

    fun submitPlans(plans: List<WorkoutPlan>, context: Context) {
        prefs = context.getSharedPreferences("workout_done", Context.MODE_PRIVATE)
        items.clear()
        for (plan in plans) {
            items.add(WorkoutItem.PlanHeader(plan))
            for (day in plan.days) {
                items.add(WorkoutItem.DayHeader(day))
                day.exercises.forEach { items.add(WorkoutItem.ExerciseItem(it, plan.planId, day.dayId)) }
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
            is WorkoutItem.ExerciseItem -> (holder as ExerciseVH).bind(item.ex, item.planId, item.dayId, prefs)
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
        fun bind(ex: Exercise, planId: Int, dayId: Int, prefs: android.content.SharedPreferences?) {
            val doneKey = "done_${planId}_${dayId}_${ex.exerciseId}"
            val isDone = prefs?.getBoolean(doneKey, false) ?: false

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

            // Video button
            if (!ex.videoUrl.isNullOrBlank()) {
                b.btnVideo.visibility = android.view.View.VISIBLE
                b.btnVideo.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ex.videoUrl))
                    it.context.startActivity(intent)
                }
            } else {
                b.btnVideo.visibility = android.view.View.GONE
            }

            // Done checkbox
            applyDoneStyle(isDone)
            b.cbDone.isChecked = isDone
            b.cbDone.setOnCheckedChangeListener { _, checked ->
                prefs?.edit()?.putBoolean(doneKey, checked)?.apply()
                applyDoneStyle(checked)
            }
        }

        private fun applyDoneStyle(done: Boolean) {
            if (done) {
                b.tvExerciseName.paintFlags = b.tvExerciseName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                b.tvExerciseName.alpha = 0.5f
                b.tvSets.alpha = 0.4f
            } else {
                b.tvExerciseName.paintFlags = b.tvExerciseName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                b.tvExerciseName.alpha = 1f
                b.tvSets.alpha = 1f
            }
        }
    }
}
