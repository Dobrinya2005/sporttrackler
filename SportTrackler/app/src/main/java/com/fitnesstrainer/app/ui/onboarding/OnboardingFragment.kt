package com.fitnesstrainer.app.ui.onboarding

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.databinding.FragmentOnboardingBinding

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private val slides = listOf(
        OnboardingSlide(
            emoji    = "📊",
            title    = "Отслеживай прогресс",
            subtitle = "Замеры тела, графики изменений и фотосравнения — всё в одном месте"
        ),
        OnboardingSlide(
            emoji    = "🏋️",
            title    = "Тренируйся с тренером",
            subtitle = "Персональные планы тренировок, чат с тренером и видеосообщения"
        ),
        OnboardingSlide(
            emoji    = "🥗",
            title    = "Питайся правильно",
            subtitle = "Дневник калорий, БЖУ и цели по питанию помогут достичь результата"
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val shown = requireContext()
            .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getBoolean("onboarding_shown", false)
        if (shown) {
            try { findNavController().navigate(R.id.action_onboarding_to_login) } catch (_: Exception) {}
            return
        }

        val adapter = OnboardingAdapter(slides)
        binding.viewPager.adapter = adapter

        val dots = listOf(binding.dot0, binding.dot1, binding.dot2)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(dots, position)
                val isLast = position == slides.lastIndex
                binding.btnNext.text = if (isLast) "Начать" else "Далее"
                binding.btnSkip.visibility = if (isLast) View.INVISIBLE else View.VISIBLE
            }
        })

        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < slides.lastIndex) {
                binding.viewPager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }

        binding.btnSkip.setOnClickListener { finishOnboarding() }
    }

    private fun updateDots(dots: List<View>, active: Int) {
        dots.forEachIndexed { i, dot ->
            val size = if (i == active) dpToPx(10) else dpToPx(8)
            dot.layoutParams = dot.layoutParams.also { lp ->
                lp.width  = size
                lp.height = size
            }
            dot.requestLayout()
            dot.background = requireContext().getDrawable(
                if (i == active) R.drawable.onboarding_dot_active
                else R.drawable.onboarding_dot_inactive
            )
        }
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    private fun finishOnboarding() {
        requireContext()
            .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("onboarding_shown", true).apply()
        findNavController().navigate(R.id.action_onboarding_to_login)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
