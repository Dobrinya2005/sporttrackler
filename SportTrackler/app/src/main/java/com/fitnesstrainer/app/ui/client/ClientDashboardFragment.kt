package com.fitnesstrainer.app.ui.client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.fitnesstrainer.app.ui.news.NewsAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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

        setupDate()
        setupClicks()
        setupObservers()

        viewModel.load()
        viewModel.loadTrainer()
        viewModel.loadAvatar()
        loadStepsAndGoals()

        binding.swipeRefresh.setColorSchemeResources(R.color.accent_cyan, R.color.accent_indigo)
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.load()
            viewModel.loadTrainer()
            viewModel.loadAvatar()
            loadStepsAndGoals()
        }

        val newsAdapter = NewsAdapter { item ->
            findNavController().navigate(
                ClientDashboardFragmentDirections.actionClientDashboardToNewsWebView(
                    url = item.url, title = item.title
                )
            )
        }
        binding.rvNews.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvNews.adapter = newsAdapter
        viewModel.news.observe(viewLifecycleOwner) { newsAdapter.submitList(it) }
        viewModel.loadNews()

        animateEntrance()
    }

    private fun setupDate() {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("ru"))
        binding.tvDate.text = today.format(formatter).replaceFirstChar { it.uppercase() }
    }

    private fun setupClicks() {
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
    }

    private fun setupObservers() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DashboardState.Loading -> {
                    binding.shimmerLayout.visibility = View.VISIBLE
                    binding.shimmerLayout.startShimmer()
                    binding.contentLayout.visibility = View.GONE
                }
                is DashboardState.Success -> {
                    binding.shimmerLayout.stopShimmer()
                    binding.shimmerLayout.visibility = View.GONE
                    binding.contentLayout.visibility = View.VISIBLE
                    binding.swipeRefresh.isRefreshing = false
                    val d = state.data
                    val firstName = d.firstName
                    binding.tvWelcome.text = "Привет, $firstName!"
                    val initials = firstName.trim().split(" ")
                        .filter { it.isNotEmpty() }.take(2)
                        .joinToString("") { it.first().uppercaseChar().toString() }
                    binding.tvAvatarInitials.text = initials.ifEmpty { firstName.take(1).uppercase() }

                    d.latest?.let { m ->
                        binding.tvWeight.text = m.weightKg?.let { "%.1f кг".format(it) } ?: "—"
                        binding.tvBmi.text    = m.bmi?.let { "ИМТ %.1f".format(it) } ?: ""
                    }

                    d.todaySummary?.let { s ->
                        binding.tvCaloriesToday.text =
                            "%.0f / %d ккал".format(s.totalCalories, s.calorieGoal ?: 0)
                        val goal = (s.calorieGoal ?: 0).toFloat()
                        binding.progressCalories.max      = 100
                        binding.progressCalories.progress = ((s.totalCalories / goal) * 100).toInt().coerceIn(0, 100)

                        val kcal = (s.calorieGoal ?: 0).toDouble()
                        val proteinGoal = s.proteinGoal ?: if (kcal > 0) (kcal * 0.20 / 4).toInt() else null
                        val fatGoal    = s.fatGoal    ?: if (kcal > 0) (kcal * 0.30 / 9).toInt() else null
                        val carbGoal   = s.carbGoal   ?: if (kcal > 0) (kcal * 0.50 / 4).toInt() else null

                        binding.tvProteinDash.text = if (proteinGoal != null)
                            "%.0f/%dг".format(s.totalProtein, proteinGoal)
                        else "%.0fг".format(s.totalProtein)
                        binding.tvFatDash.text = if (fatGoal != null)
                            "%.0f/%dг".format(s.totalFat, fatGoal)
                        else "%.0fг".format(s.totalFat)
                        binding.tvCarbsDash.text = if (carbGoal != null)
                            "%.0f/%dг".format(s.totalCarbs, carbGoal)
                        else "%.0fг".format(s.totalCarbs)
                    }
                }
                is DashboardState.Error -> {
                    binding.shimmerLayout.stopShimmer()
                    binding.shimmerLayout.visibility = View.GONE
                    binding.contentLayout.visibility = View.VISIBLE
                    binding.swipeRefresh.isRefreshing = false
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_INDEFINITE)
                        .setAction("Повторить") { viewModel.load() }
                        .show()
                }
            }
        }

        viewModel.avatarUrl.observe(viewLifecycleOwner) { url ->
            if (!url.isNullOrBlank()) {
                binding.ivAvatar.visibility = View.VISIBLE
                binding.tvAvatarInitials.visibility = View.GONE
                Glide.with(this)
                    .load(url)
                    .transform(CircleCrop())
                    .into(binding.ivAvatar)
            } else {
                binding.ivAvatar.visibility = View.GONE
                binding.tvAvatarInitials.visibility = View.VISIBLE
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
    }

    private fun animateEntrance() {
        val interp = DecelerateInterpolator(1.5f)
        val views = listOf(
            binding.header,
            binding.sectionNews,
            binding.labelSections,
            binding.navGrid
        )
        views.forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = 32f
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(450)
                .setStartDelay(i * 70L)
                .setInterpolator(interp)
                .start()
        }
    }

    private fun loadStepsAndGoals() {
        lifecycleScope.launch {
            val db      = App.instance.database
            val uid     = App.instance.tokenStorage.getUserId()
            val today   = LocalDate.now().toString()

            val steps = db.stepDao().get(uid, today)?.steps ?: 0
            _binding?.tvSteps?.text = steps.toString()

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
            _binding?.tvGoalsSummary?.text = when {
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

    override fun onResume() {
        super.onResume()
        loadStepsAndGoals()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
