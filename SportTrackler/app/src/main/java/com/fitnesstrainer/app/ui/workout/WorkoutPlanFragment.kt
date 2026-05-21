package com.fitnesstrainer.app.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitnesstrainer.app.databinding.FragmentWorkoutPlanBinding

class WorkoutPlanFragment : Fragment() {

    private var _binding: FragmentWorkoutPlanBinding? = null
    private val binding get() = _binding!!
    private val args: WorkoutPlanFragmentArgs by navArgs()
    private val viewModel: WorkoutPlanViewModel by viewModels()
    private lateinit var adapter: WorkoutPlanAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = WorkoutPlanAdapter()
        binding.rvPlan.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlan.adapter       = adapter

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }
        binding.btnRetry.setOnClickListener { viewModel.load() }

        // FAB виден только тренеру (clientId != -1 означает тренер просматривает клиента)
        if (args.clientId != -1) {
            binding.fabCreatePlan.visibility = View.VISIBLE
            binding.fabCreatePlan.setOnClickListener {
                val action = WorkoutPlanFragmentDirections
                    .actionWorkoutPlanToCreatePlan(args.clientId)
                findNavController().navigate(action)
            }
        }

        viewModel.init(args.clientId)

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.swipeRefresh.isRefreshing = false
            when (state) {
                is WorkoutState.Loading -> {
                    binding.swipeRefresh.isRefreshing = true
                    binding.emptyState.visibility = View.GONE
                }
                is WorkoutState.Success -> {
                    adapter.submitPlans(state.plans, requireContext())
                    if (state.plans.isEmpty()) {
                        binding.tvEmpty.text    = "Тренировочный план не назначен"
                        binding.tvEmptySub.text = if (args.clientId != -1)
                            "Нажмите + чтобы создать план для клиента"
                        else
                            "Тренер назначит план в ближайшее время"
                        binding.btnRetry.visibility   = View.GONE
                        binding.emptyState.visibility = View.VISIBLE
                    } else {
                        binding.emptyState.visibility = View.GONE
                    }
                }
                is WorkoutState.Error -> {
                    binding.tvEmpty.text    = state.message
                    binding.tvEmptySub.text = ""
                    binding.btnRetry.visibility   = View.VISIBLE
                    binding.emptyState.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
