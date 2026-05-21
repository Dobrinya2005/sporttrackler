package com.fitnesstrainer.app.ui.onboarding

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.model.SaveClientProfileRequest
import com.fitnesstrainer.app.databinding.FragmentClientOnboardingBinding
import kotlinx.coroutines.launch


class ClientOnboardingFragment : Fragment() {

    private var _b: FragmentClientOnboardingBinding? = null
    private val b get() = _b!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentClientOnboardingBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.btnNext.setOnClickListener { saveAndNext() }
        b.btnSkip.setOnClickListener { navigateToMeasurements() }

        prefillFromProfile()
    }

    private fun prefillFromProfile() {
        val w = arguments?.getFloat("weightKg") ?: -1f
        val h = arguments?.getFloat("heightCm") ?: -1f
        if (w > 0) b.etWeight.setText(w.toBigDecimal().stripTrailingZeros().toPlainString())
        if (h > 0) b.etHeight.setText(h.toBigDecimal().stripTrailingZeros().toPlainString())
    }

    private fun saveAndNext() {
        val goal = when (b.rgGoal.checkedRadioButtonId) {
            R.id.rb_goal_lose      -> "lose_weight"
            R.id.rb_goal_gain      -> "gain_muscle"
            R.id.rb_goal_maintain  -> "maintain"
            R.id.rb_goal_endurance -> "endurance"
            else -> null
        }
        val activity = when (b.rgActivity.checkedRadioButtonId) {
            R.id.rb_activity_beginner     -> "beginner"
            R.id.rb_activity_intermediate -> "intermediate"
            R.id.rb_activity_advanced     -> "advanced"
            else -> null
        }
        val weight     = b.etWeight.text?.toString()?.toDoubleOrNull()
        val height     = b.etHeight.text?.toString()?.toDoubleOrNull()
        val goalWeight = b.etGoalWeight.text?.toString()?.toDoubleOrNull()

        setLoading(true)
        lifecycleScope.launch {
            try {
                val api = (requireActivity().application as App).apiService
                api.saveClientProfile(
                    SaveClientProfileRequest(goal, activity, null, height, goalWeight, null)
                )
            } catch (_: Exception) {
                // Non-critical — navigate anyway
            } finally {
                setLoading(false)
                if (weight != null) {
                    requireContext()
                        .getSharedPreferences("onboarding", Context.MODE_PRIVATE)
                        .edit().putFloat("initial_weight", weight.toFloat()).apply()
                }
                navigateToMeasurements()
            }
        }
    }

    private fun navigateToMeasurements() {
        findNavController().navigate(R.id.action_clientOnboarding_to_onboardingMeasurements)
    }

    private fun setLoading(loading: Boolean) {
        b.progress.visibility = if (loading) View.VISIBLE else View.GONE
        b.btnNext.isEnabled   = !loading
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
