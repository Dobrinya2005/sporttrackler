package com.fitnesstrainer.app.ui.trainer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.databinding.FragmentTrainerDashboardBinding
import com.fitnesstrainer.app.ui.news.NewsAdapter

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
        binding.btnRetry.setOnClickListener { viewModel.loadClients() }

        binding.btnLogout.onClick = {
            viewModel.logout()
            findNavController().navigate(
                R.id.loginFragment, null,
                NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
            )
        }

        viewModel.trainerName.observe(viewLifecycleOwner) { name ->
            binding.tvTrainerName.text = name
            val initials = name.trim().split(" ")
                .filter { it.isNotEmpty() }
                .take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }
            binding.tvAvatarInitials.text = initials
        }

        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            binding.tvClientCount.text = stats.clientCount.toString()
            binding.tvRating.text = stats.averageRating?.let { "%.1f".format(it) } ?: "—"
            binding.tvReviewCount.text = stats.reviewCount.toString()
        }

        val newsAdapter = NewsAdapter { item ->
            findNavController().navigate(
                TrainerDashboardFragmentDirections.actionTrainerDashboardToNewsWebView(
                    url = item.url, title = item.title
                )
            )
        }
        binding.rvNews.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvNews.adapter = newsAdapter
        viewModel.news.observe(viewLifecycleOwner) { newsAdapter.submitList(it) }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.swipeRefresh.isRefreshing = false
            when (state) {
                is TrainerState.Loading -> {
                    binding.swipeRefresh.isRefreshing = true
                    binding.emptyState.visibility = View.GONE
                }
                is TrainerState.Success -> {
                    adapter.submitList(state.clients)
                    val isEmpty = state.clients.isEmpty()
                    binding.rvClients.visibility = if (isEmpty) View.GONE else View.VISIBLE
                    if (isEmpty) {
                        binding.tvEmpty.text    = "Клиентов пока нет"
                        binding.tvEmptySub.text = "Клиенты появятся после регистрации"
                        binding.btnRetry.visibility   = View.GONE
                        binding.emptyState.visibility = View.VISIBLE
                    } else {
                        binding.emptyState.visibility = View.GONE
                    }
                }
                is TrainerState.Error -> {
                    binding.rvClients.visibility  = View.GONE
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
