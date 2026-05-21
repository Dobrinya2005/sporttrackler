package com.fitnesstrainer.app.ui.trainer

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.databinding.FragmentTrainerStatsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class TrainerStatsFragment : Fragment() {

    private var _binding: FragmentTrainerStatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TrainerStatsViewModel by viewModels()
    private lateinit var weightAdapter: ClientWeightAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainerStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        weightAdapter = ClientWeightAdapter()
        binding.rvClientWeights.layoutManager = LinearLayoutManager(requireContext())
        binding.rvClientWeights.adapter = weightAdapter

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is StatsState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is StatsState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    bindData(state.data)
                }
                is StatsState.Error -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun bindData(data: TrainerStatsData) {
        binding.tvClientCount.text = data.clientCount.toString()
        binding.tvAvgRating.text   = data.avgRating?.let { "%.1f".format(it) } ?: "—"
        binding.tvReviewCount.text = data.reviewCount.toString()

        setupRatingsChart(data.ratingDistribution)

        val withWeights = data.clientWeights.filter { it.latestMeasurement?.weightKg != null }
        if (withWeights.isNotEmpty()) {
            binding.tvClientsLabel.visibility   = View.VISIBLE
            binding.rvClientWeights.visibility  = View.VISIBLE
            weightAdapter.submitList(data.clientWeights)
        }
    }

    private fun setupRatingsChart(dist: Map<Int, Int>) {
        val labels  = listOf("1★", "2★", "3★", "4★", "5★")
        val colors  = listOf(
            Color.parseColor("#EF4444"),
            Color.parseColor("#F97316"),
            Color.parseColor("#EAB308"),
            Color.parseColor("#22C55E"),
            Color.parseColor("#06B6D4")
        )

        val entries = (1..5).map { star ->
            BarEntry(star.toFloat() - 1f, (dist[star] ?: 0).toFloat())
        }

        val dataSet = BarDataSet(entries, "").apply {
            setColors(colors)
            setDrawValues(true)
            valueTextColor = resources.getColor(R.color.text_hint, null)
            valueTextSize  = 11f
        }

        binding.chartRatings.apply {
            data = BarData(dataSet).also { it.barWidth = 0.6f }
            xAxis.apply {
                valueFormatter   = IndexAxisValueFormatter(labels)
                position         = XAxis.XAxisPosition.BOTTOM
                granularity      = 1f
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor        = resources.getColor(R.color.text_hint, null)
                textSize         = 12f
            }
            axisLeft.apply {
                granularity      = 1f
                setDrawGridLines(true)
                gridColor        = Color.argb(30, 255, 255, 255)
                setDrawAxisLine(false)
                textColor        = resources.getColor(R.color.text_hint, null)
                axisMinimum      = 0f
            }
            axisRight.isEnabled   = false
            legend.isEnabled      = false
            description.isEnabled = false
            setBackgroundColor(Color.TRANSPARENT)
            setDrawGridBackground(false)
            setTouchEnabled(false)
            setExtraOffsets(8f, 16f, 8f, 8f)
            animateY(600)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
