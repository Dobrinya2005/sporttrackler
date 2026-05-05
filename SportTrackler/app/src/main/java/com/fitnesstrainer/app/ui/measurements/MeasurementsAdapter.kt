package com.fitnesstrainer.app.ui.measurements

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitnesstrainer.app.data.model.MeasurementResponse
import com.fitnesstrainer.app.databinding.ItemMeasurementBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MeasurementsAdapter(
    private val onDelete: (Int) -> Unit
) : ListAdapter<MeasurementResponse, MeasurementsAdapter.VH>(DIFF) {

    var canDelete = true

    inner class VH(private val b: ItemMeasurementBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: MeasurementResponse) {
            val date = try {
                LocalDateTime.parse(item.measuredAt, DateTimeFormatter.ISO_DATE_TIME)
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            } catch (_: Exception) { item.measuredAt.take(10) }

            b.tvDate.text   = date
            b.tvWeight.text = item.weightKg?.let { "%.1f кг".format(it) } ?: "—"
            b.tvBmi.text    = item.bmi?.let { "ИМТ %.1f".format(it) } ?: ""

            val parts = mutableListOf<String>()
            item.chestCm?.let  { parts.add("Грудь: %.0f".format(it)) }
            item.waistCm?.let  { parts.add("Талия: %.0f".format(it)) }
            item.hipsCm?.let   { parts.add("Бёдра: %.0f".format(it)) }
            item.bicepCm?.let  { parts.add("Бицепс: %.0f".format(it)) }
            b.tvExtra.text = parts.joinToString("  ·  ")

            b.btnDelete.isEnabled = canDelete
            b.btnDelete.setOnClickListener { onDelete(item.measurementId) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemMeasurementBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MeasurementResponse>() {
            override fun areItemsTheSame(a: MeasurementResponse, b: MeasurementResponse) =
                a.measurementId == b.measurementId
            override fun areContentsTheSame(a: MeasurementResponse, b: MeasurementResponse) = a == b
        }
    }
}
