package com.fitnesstrainer.app.ui.onboarding

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.model.MeasurementRequest
import com.fitnesstrainer.app.databinding.FragmentOnboardingMeasurementsBinding
import kotlinx.coroutines.launch

class OnboardingMeasurementsFragment : Fragment() {

    private var _b: FragmentOnboardingMeasurementsBinding? = null
    private val b get() = _b!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentOnboardingMeasurementsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        b.btnSave.setOnClickListener { save() }
        b.btnSkip.setOnClickListener { navigateToPhotos() }

        val prefs = requireContext().getSharedPreferences("onboarding", Context.MODE_PRIVATE)
        val savedWeight = prefs.getFloat("initial_weight", -1f).takeIf { it > 0 }
        if (savedWeight != null) {
            b.etWeight.setText(savedWeight.toBigDecimal().stripTrailingZeros().toPlainString())
        }
        prefs.edit().remove("initial_weight").apply()
    }

    private fun save() {
        val weight  = b.etWeight.text?.toString()?.toDoubleOrNull()
        val chest   = b.etChest.text?.toString()?.toDoubleOrNull()
        val waist   = b.etWaist.text?.toString()?.toDoubleOrNull()
        val hips    = b.etHips.text?.toString()?.toDoubleOrNull()
        val bicep   = b.etBicep.text?.toString()?.toDoubleOrNull()
        val bodyFat = b.etBodyFat.text?.toString()?.toDoubleOrNull()

        if (weight == null && chest == null && waist == null && hips == null && bicep == null) {
            b.tvError.text = "Введите хотя бы один замер"
            b.tvError.visibility = View.VISIBLE
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                (requireActivity().application as App).apiService.addMeasurement(
                    MeasurementRequest(
                        weightKg   = weight,
                        chestCm    = chest,
                        waistCm    = waist,
                        hipsCm     = hips,
                        bicepCm    = bicep,
                        bodyFatPercent = bodyFat
                    )
                )
                navigateToPhotos()
            } catch (_: Exception) {
                b.tvError.text = "Ошибка сохранения"
                b.tvError.visibility = View.VISIBLE
            } finally {
                setLoading(false)
            }
        }
    }

    private fun navigateToPhotos() {
        findNavController().navigate(R.id.action_onboardingMeasurements_to_onboardingPhotos)
    }

    private fun setLoading(loading: Boolean) {
        b.progress.visibility = if (loading) View.VISIBLE else View.GONE
        b.btnSave.isEnabled   = !loading
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
