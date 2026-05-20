package com.fitnesstrainer.app.ui.client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.databinding.DialogLeaveReviewBinding
import com.fitnesstrainer.app.databinding.FragmentClientDashboardBinding
import com.fitnesstrainer.app.ui.news.NewsAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.time.LocalDate

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
        binding.btnScanQr.setOnClickListener {
            findNavController().navigate(R.id.action_clientDashboard_to_qrScan)
        }
        binding.cardGoals.setOnClickListener {
            findNavController().navigate(R.id.action_clientDashboard_to_goals)
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DashboardState.Loading -> binding.progressBar.visibility = View.VISIBLE
                is DashboardState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val d = state.data
                    binding.tvWelcome.text = "Привет, ${d.firstName}!"

                    d.latest?.let { m ->
                        binding.tvWeight.text = m.weightKg?.let { "%.1f кг".format(it) } ?: "—"
                        binding.tvBmi.text    = m.bmi?.let { "ИМТ %.1f".format(it) } ?: ""
                    }

                    d.todaySummary?.let { s ->
                        binding.tvCaloriesToday.text =
                            "%.0f / %d ккал".format(s.totalCalories, s.calorieGoal ?: 2000)
                        val goal = (s.calorieGoal ?: 2000).toFloat()
                        binding.progressCalories.max      = 100
                        binding.progressCalories.progress = ((s.totalCalories / goal) * 100).toInt().coerceIn(0, 100)

                        binding.tvProteinDash.text = if (s.proteinGoal != null)
                            "%.0f/%.0f г".format(s.totalProtein, s.proteinGoal)
                        else "%.0f г".format(s.totalProtein)
                        binding.tvFatDash.text = if (s.fatGoal != null)
                            "%.0f/%.0f г".format(s.totalFat, s.fatGoal)
                        else "%.0f г".format(s.totalFat)
                        binding.tvCarbsDash.text = if (s.carbGoal != null)
                            "%.0f/%.0f г".format(s.totalCarbs, s.carbGoal)
                        else "%.0f г".format(s.totalCarbs)
                    }
                }
                is DashboardState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_INDEFINITE)
                        .setAction("Повторить") { viewModel.load() }
                        .show()
                }
            }
        }

        viewModel.myTrainer.observe(viewLifecycleOwner) { trainer ->
            if (trainer != null) {
                binding.cardReview.visibility = View.VISIBLE
                binding.btnLeaveReview.setOnClickListener {
                    showReviewDialog(trainer.userId, "${trainer.firstName} ${trainer.lastName}")
                }
            } else {
                binding.cardReview.visibility = View.GONE
            }
        }

        viewModel.load()
        viewModel.loadTrainer()
        loadStepsAndGoals()

        val newsAdapter = NewsAdapter()
        binding.rvNews.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvNews.adapter = newsAdapter
        viewModel.news.observe(viewLifecycleOwner) { newsAdapter.submitList(it) }
        viewModel.loadNews()
    }

    private fun loadStepsAndGoals() {
        lifecycleScope.launch {
            val db      = App.instance.database
            val uid     = App.instance.tokenStorage.getUserId()
            val today   = LocalDate.now().toString()

            val steps = db.stepDao().get(uid, today)?.steps ?: 0
            binding.tvSteps.text = steps.toString()

            val goals = db.goalDao().getAll(uid)
            val done  = goals.count { goal ->
                val latest = db.measurementDao().getAll(uid).firstOrNull()
                val cur = latest?.let { m -> when (goal.type) {
                    "WEIGHT"    -> m.weightKg
                    "BODY_FAT"  -> m.bodyFatPercent
                    "CHEST"     -> m.chestCm
                    "WAIST"     -> m.waistCm
                    "HIPS"      -> m.hipsCm
                    "BICEP"     -> m.bicepCm
                    else        -> null
                }}
                cur != null && cur <= goal.targetValue
            }
            binding.tvGoalsSummary.text = when {
                goals.isEmpty() -> "Нет целей"
                done == goals.size -> "Все достигнуты ✓"
                else -> "$done / ${goals.size} достигнуто"
            }
        }
    }

    private fun showReviewDialog(trainerId: Int, trainerName: String) {
        val dialogBinding = DialogLeaveReviewBinding.inflate(layoutInflater)
        dialogBinding.tvTrainerName.text = trainerName
        AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Отправить") { _, _ ->
                val rating  = dialogBinding.ratingBar.rating.toInt()
                val comment = dialogBinding.etComment.text?.toString()?.trim()
                viewModel.submitReview(trainerId, rating, comment)
                Toast.makeText(requireContext(), "Отзыв отправлен!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
