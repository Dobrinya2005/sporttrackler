package com.fitnesstrainer.app.ui.trainer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.fitnesstrainer.app.databinding.FragmentClientDetailBinding

class ClientDetailFragment : Fragment() {

    private var _binding: FragmentClientDetailBinding? = null
    private val binding get() = _binding!!
    private val args: ClientDetailFragmentArgs by navArgs()
    private val viewModel: ClientDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvClientName.text = args.clientName.ifBlank { "Клиент #${args.clientId}" }
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.btnMeasurements.setOnClickListener {
            val action = ClientDetailFragmentDirections.actionClientDetailToMeasurements(args.clientId)
            findNavController().navigate(action)
        }
        binding.btnPhotos.setOnClickListener {
            val action = ClientDetailFragmentDirections.actionClientDetailToPhotos(args.clientId)
            findNavController().navigate(action)
        }
        binding.btnWorkout.setOnClickListener {
            val action = ClientDetailFragmentDirections.actionClientDetailToWorkoutPlan(args.clientId)
            findNavController().navigate(action)
        }
        binding.btnChat.setOnClickListener {
            val action = ClientDetailFragmentDirections.actionClientDetailToChat(
                contactId   = args.clientId,
                contactName = args.clientName
            )
            findNavController().navigate(action)
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ClientDetailState.Loading -> binding.progressBar.visibility = View.VISIBLE
                is ClientDetailState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val m = state.latest
                    if (m != null) {
                        binding.tvWeight.text    = m.weightKg?.let { "%.1f кг".format(it) } ?: "—"
                        binding.tvBmi.text       = m.bmi?.let { "ИМТ: %.1f".format(it) } ?: ""
                        binding.tvBodyFat.text   = m.bodyFatPercent?.let { "Жир: %.1f%%".format(it) } ?: ""
                        binding.cardLatest.visibility = View.VISIBLE
                    } else {
                        binding.cardLatest.visibility = View.GONE
                    }
                }
                is ClientDetailState.Error -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }

        viewModel.loadLatest(args.clientId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
