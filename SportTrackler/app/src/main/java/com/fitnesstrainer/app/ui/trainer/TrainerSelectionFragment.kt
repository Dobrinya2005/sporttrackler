package com.fitnesstrainer.app.ui.trainer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.databinding.FragmentTrainerSelectionBinding

class TrainerSelectionFragment : Fragment() {

    private var _b: FragmentTrainerSelectionBinding? = null
    private val b get() = _b!!
    private val viewModel: TrainerSelectionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentTrainerSelectionBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = TrainerSelectionAdapter { trainer ->
            viewModel.selectTrainer(trainer.userId)
        }

        b.rvTrainers.layoutManager = LinearLayoutManager(requireContext())
        b.rvTrainers.adapter = adapter

        b.btnSkip.setOnClickListener { goToDashboard() }
        b.btnScanQr.setOnClickListener {
            findNavController().navigate(R.id.action_trainerSelection_to_qrScan)
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TrainerSelectionState.Loading -> b.progress.visibility = View.VISIBLE
                is TrainerSelectionState.Success -> {
                    b.progress.visibility = View.GONE
                    adapter.submitList(state.trainers)
                }
                is TrainerSelectionState.Error -> {
                    b.progress.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.selectResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is SelectResult.Success -> {
                    Toast.makeText(requireContext(), "Тренер выбран!", Toast.LENGTH_SHORT).show()
                    viewModel.clearSelectResult()
                    goToDashboard()
                }
                is SelectResult.Error -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                    viewModel.clearSelectResult()
                }
                else -> {}
            }
        }
    }

    private fun goToDashboard() {
        findNavController().navigate(
            R.id.clientDashboardFragment, null,
            NavOptions.Builder()
                .setPopUpTo(R.id.loginFragment, true)
                .build()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
