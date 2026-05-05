package com.fitnesstrainer.app.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

        viewModel.init(args.clientId)

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.swipeRefresh.isRefreshing = false
            when (state) {
                is WorkoutState.Loading -> binding.swipeRefresh.isRefreshing = true
                is WorkoutState.Success -> {
                    adapter.submitPlans(state.plans)
                    binding.tvEmpty.visibility =
                        if (state.plans.isEmpty()) View.VISIBLE else View.GONE
                }
                is WorkoutState.Error ->
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
