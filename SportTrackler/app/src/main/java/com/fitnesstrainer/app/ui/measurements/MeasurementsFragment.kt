package com.fitnesstrainer.app.ui.measurements

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.model.MeasurementRequest
import com.fitnesstrainer.app.data.model.MeasurementResponse
import com.fitnesstrainer.app.databinding.DialogAddMeasurementBinding
import com.fitnesstrainer.app.databinding.FragmentMeasurementsBinding
import com.fitnesstrainer.app.util.PdfExporter
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class MeasurementsFragment : Fragment() {

    private var _binding: FragmentMeasurementsBinding? = null
    private val binding get() = _binding!!
    private val args: MeasurementsFragmentArgs by navArgs()
    private val viewModel: MeasurementsViewModel by viewModels()
    private lateinit var adapter: MeasurementsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMeasurementsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MeasurementsAdapter { id -> viewModel.delete(id) }
        binding.rvMeasurements.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMeasurements.adapter       = adapter

        binding.btnBack.setOnClickListener    { findNavController().popBackStack() }
        binding.fabAdd.setOnClickListener    { showAddDialog() }
        binding.btnRetry.setOnClickListener  { viewModel.load() }
        binding.btnExport.setOnClickListener { exportPdf() }

        viewModel.init(args.clientId)

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MeasurementsState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.emptyState.visibility  = View.GONE
                }
                is MeasurementsState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    adapter.canDelete = viewModel.isOwnData
                    adapter.submitList(state.list)
                    if (state.list.isEmpty()) {
                        binding.tvEmpty.text    = "Замеров пока нет"
                        binding.tvEmptySub.text = "Нажмите + чтобы добавить первый замер"
                        binding.btnRetry.visibility = View.GONE
                        binding.emptyState.visibility = View.VISIBLE
                    } else {
                        binding.emptyState.visibility = View.GONE
                    }
                    updateChart(state.list.map { it })
                }
                is MeasurementsState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.text    = state.message
                    binding.tvEmptySub.text = ""
                    binding.btnRetry.visibility   = View.VISIBLE
                    binding.emptyState.visibility = View.VISIBLE
                }
            }
        }

        viewModel.saved.observe(viewLifecycleOwner) {
            if (it == true) Toast.makeText(requireContext(), "Замер сохранён", Toast.LENGTH_SHORT).show()
        }
    }

    private var currentList: List<MeasurementResponse> = emptyList()

    private fun updateChart(list: List<MeasurementResponse>) {
        currentList = list
        if (list.isEmpty()) { binding.chart.visibility = View.GONE; return }
        binding.chart.visibility = View.VISIBLE

        val sorted  = list.sortedBy { it.measuredAt }
        val entries = sorted.mapIndexedNotNull { i, m ->
            m.weightKg?.let { Entry(i.toFloat(), it.toFloat()) }
        }
        if (entries.isEmpty()) { binding.chart.visibility = View.GONE; return }

        val labels = sorted.map { it.measuredAt.take(5) }

        val dataSet = LineDataSet(entries, "Вес (кг)").apply {
            color         = resources.getColor(R.color.accent_cyan, null)
            setCircleColor(resources.getColor(R.color.accent_cyan, null))
            lineWidth     = 2f
            circleRadius  = 4f
            setDrawValues(false)
            // Выделяем точки с заметками
            val highlighted = sorted.mapIndexedNotNull { i, m ->
                if (!m.notes.isNullOrBlank()) i else null
            }.toSet()
            circleHoleColor = resources.getColor(R.color.bg_elevated, null)
        }

        val marker = MeasurementMarkerView(requireContext(), sorted)
        binding.chart.apply {
            marker.chartView = this
            this.marker  = marker
            data = LineData(dataSet)
            xAxis.apply {
                valueFormatter    = IndexAxisValueFormatter(labels)
                position          = XAxis.XAxisPosition.BOTTOM
                granularity       = 1f
                textColor         = resources.getColor(R.color.text_hint, null)
                setDrawGridLines(false)
            }
            axisLeft.textColor  = resources.getColor(R.color.text_hint, null)
            axisRight.isEnabled = false
            legend.textColor    = resources.getColor(R.color.text_primary, null)
            description.isEnabled = false
            setBackgroundColor(resources.getColor(R.color.bg_elevated, null))
            animateX(500)
            invalidate()
        }
    }

    private fun exportPdf() {
        if (currentList.isEmpty()) {
            Toast.makeText(requireContext(), "Нет данных для экспорта", Toast.LENGTH_SHORT).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val firstName = App.instance.tokenStorage.getFirstName() ?: ""
            PdfExporter.exportMeasurements(requireContext(), currentList, firstName)
        }
    }

    private fun showAddDialog() {
        val dialogBinding = DialogAddMeasurementBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Новый замер")
            .setView(dialogBinding.root)
            .setPositiveButton("Сохранить", null)
            .setNegativeButton("Отмена", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                fun d(s: String) = s.trim().toDoubleOrNull()
                val weight     = d(dialogBinding.etWeight.text.toString())
                val height     = d(dialogBinding.etHeight.text.toString())
                val chest      = d(dialogBinding.etChest.text.toString())
                val waist      = d(dialogBinding.etWaist.text.toString())
                val hips       = d(dialogBinding.etHips.text.toString())
                val bicep      = d(dialogBinding.etBicep.text.toString())
                val bodyFat    = d(dialogBinding.etBodyFat.text.toString())

                val error = validateMeasurements(weight, height, chest, waist, hips, bicep, bodyFat)
                if (error != null) {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                viewModel.addMeasurement(MeasurementRequest(
                    weightKg       = weight,
                    heightCm       = height,
                    chestCm        = chest,
                    waistCm        = waist,
                    hipsCm         = hips,
                    bicepCm        = bicep,
                    bodyFatPercent = bodyFat,
                    notes          = dialogBinding.etNotes.text.toString().trim().ifBlank { null }
                ))
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun validateMeasurements(
        weight: Double?, height: Double?, chest: Double?,
        waist: Double?, hips: Double?, bicep: Double?, bodyFat: Double?
    ): String? = when {
        weight != null && (weight < 20 || weight > 300)   -> "Вес должен быть от 20 до 300 кг"
        height != null && (height < 50 || height > 250)   -> "Рост должен быть от 50 до 250 см"
        chest  != null && (chest  < 40 || chest  > 200)   -> "Обхват груди должен быть от 40 до 200 см"
        waist  != null && (waist  < 30 || waist  > 200)   -> "Обхват талии должен быть от 30 до 200 см"
        hips   != null && (hips   < 40 || hips   > 200)   -> "Обхват бёдер должен быть от 40 до 200 см"
        bicep  != null && (bicep  < 10 || bicep  > 80)    -> "Обхват бицепса должен быть от 10 до 80 см"
        bodyFat != null && (bodyFat < 1 || bodyFat > 60)  -> "Процент жира должен быть от 1 до 60%"
        else -> null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
