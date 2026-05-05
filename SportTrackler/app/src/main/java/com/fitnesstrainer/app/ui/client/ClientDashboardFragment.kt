package com.fitnesstrainer.app.ui.client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.databinding.FragmentClientDashboardBinding

class ClientDashboardFragment : Fragment() {

    private var _binding: FragmentClientDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ClientDashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardMeasurements.setOnClickListener {
            findNavController().navigate(
                ClientDashboardFragmentDirections.actionClientDashboardToMeasurements(-1)
            )
        }
        binding.cardFood.setOnClickListener {
            findNavController().navigate(R.id.action_clientDashboard_to_food)
        }
        binding.cardPhotos.setOnClickListener {
            findNavController().navigate(
                ClientDashboardFragmentDirections.actionClientDashboardToPhotos(-1)
            )
        }
        binding.cardWorkout.setOnClickListener {
            findNavController().navigate(
                ClientDashboardFragmentDirections.actionClientDashboardToWorkout(-1)
            )
        }
        binding.cardChat.setOnClickListener { viewModel.openChat() }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            findNavController().navigate(
                R.id.loginFragment, null,
                NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
            )
        }

        viewModel.navigateToChat.observe(viewLifecycleOwner) { pair ->
            if (pair != null) {
                val action = ClientDashboardFragmentDirections.actionClientDashboardToChat(
                    contactId   = pair.first,
                    contactName = pair.second
                )
                findNavController().navigate(action)
                viewModel.clearNavigation()
            }
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DashboardState.Loading -> binding.progressBar.visibility = View.VISIBLE
                is DashboardState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val d = state.data
                    binding.tvWelcome.text = "Привет, ${d.firstName}!"

                    d.latest?.let { m ->
                        binding.tvWeight.text  = m.weightKg?.let { "%.1f кг".format(it) } ?: "—"
                        binding.tvBmi.text     = m.bmi?.let { "ИМТ %.1f".format(it) } ?: ""
                    }

                    d.todaySummary?.let { s ->
                        binding.tvCaloriesToday.text =
                            "%.0f / %d ккал".format(s.totalCalories, s.calorieGoal ?: 2000)
                        val goal = (s.calorieGoal ?: 2000).toFloat()
                        binding.progressCalories.max      = 100
                        binding.progressCalories.progress = ((s.totalCalories / goal) * 100).toInt().coerceIn(0, 100)
                    }
                }
                is DashboardState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
