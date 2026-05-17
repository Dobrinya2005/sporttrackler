package com.fitnesstrainer.app.ui.measurements

import android.content.Context
import android.widget.TextView
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.model.MeasurementResponse
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class MeasurementMarkerView(
    context: Context,
    private val measurements: List<MeasurementResponse>
) : MarkerView(context, R.layout.view_chart_marker) {

    private val tvDate   = findViewById<TextView>(R.id.tv_marker_date)
    private val tvWeight = findViewById<TextView>(R.id.tv_marker_weight)
    private val tvNote   = findViewById<TextView>(R.id.tv_marker_note)

    override fun refreshContent(e: Entry, highlight: Highlight) {
        val index = e.x.toInt().coerceIn(0, measurements.size - 1)
        val m     = measurements[index]
        tvDate.text   = m.measuredAt.take(10)
        tvWeight.text = m.weightKg?.let { "%.1f кг".format(it) } ?: ""
        tvNote.text   = m.notes ?: ""
        tvNote.visibility = if (m.notes.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF =
        MPPointF(-(width / 2f), -height.toFloat() - 16f)
}
