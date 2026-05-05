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
import com.fitnesstrainer.app.databinding.FragmentTrainerDashboardBinding

class TrainerDashboardFragment : Fragment() {

    private var _binding: FragmentTrainerDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TrainerViewModel by viewModels()
    private lateinit var adapter: ClientsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainerDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ClientsAdapter(
            onClick = { client ->
                val action = TrainerDashboardFragmentDirections
                    .actionTrainerDashboardToClientDetail(
                        clientId   = client.userId,
                        clientName = "${client.firstName} ${client.lastName}"
                    )
                findNavController().navigate(action)
            },
            onChatClick = { client ->
                val action = TrainerDashboardFragmentDirections
                    .actionTrainerDashboardToChat(
                        contactId   = client.userId,
                        contactName = "${client.firstName} ${client.lastName}"
                    )
                findNavController().navigate(action)
            }
        )
        binding.rvClients.layoutManager = LinearLayoutManager(requireContext())
        binding.rvClients.adapter       = adapter

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadClients() }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            findNavController().navigate(
                R.id.loginFragment, null,
                NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
            )
        }

        viewModel.trainerName.observe(viewLifecycleOwner) { binding.tvTrainerName.text = it }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.swipeRefresh.isRefreshing = false
            when (state) {
                is TrainerState.Loading -> binding.swipeRefresh.isRefreshing = true
                is TrainerState.Success -> {
                    adapter.submitList(state.clients)
                    binding.tvEmpty.visibility =
                        if (state.clients.isEmpty()) View.VISIBLE else View.GONE
                }
                is TrainerState.Error ->
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
