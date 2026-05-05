package com.fitnesstrainer.app.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.local.ThemeManager
import com.fitnesstrainer.app.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        startSplashAnimation()
    }

    private fun startSplashAnimation() {
        // Phase 1: Logo bounce-in + slight rotation (0–750ms)
        val logoScaleX = ObjectAnimator.ofFloat(binding.logo, View.SCALE_X, 0.3f, 1.15f, 1f).apply {
            duration = 750; interpolator = OvershootInterpolator(2.0f)
        }
        val logoScaleY = ObjectAnimator.ofFloat(binding.logo, View.SCALE_Y, 0.3f, 1.15f, 1f).apply {
            duration = 750; interpolator = OvershootInterpolator(2.0f)
        }
        val logoAlpha = ObjectAnimator.ofFloat(binding.logo, View.ALPHA, 0f, 1f).apply {
            duration = 500
        }
        val logoRotate = ObjectAnimator.ofFloat(binding.logo, View.ROTATION, -15f, 5f, 0f).apply {
            duration = 750; interpolator = OvershootInterpolator(1.5f)
        }

        // Glow pulse
        val glowAlpha = ObjectAnimator.ofFloat(binding.glow, View.ALPHA, 0f, 0.7f).apply {
            duration = 900; startDelay = 150
        }
        val glowScale = ObjectAnimator.ofFloat(binding.glow, View.SCALE_X, 0.8f, 1.1f, 1f).apply {
            duration = 900; startDelay = 150; interpolator = DecelerateInterpolator()
        }
        val glowScaleY = ObjectAnimator.ofFloat(binding.glow, View.SCALE_Y, 0.8f, 1.1f, 1f).apply {
            duration = 900; startDelay = 150; interpolator = DecelerateInterpolator()
        }

        // Phase 2: App name slides up
        val titleAlpha = ObjectAnimator.ofFloat(binding.appName, View.ALPHA, 0f, 1f).apply {
            duration = 500; startDelay = 450
        }
        val titleY = ObjectAnimator.ofFloat(binding.appName, View.TRANSLATION_Y, 30f, 0f).apply {
            duration = 500; startDelay = 450; interpolator = DecelerateInterpolator()
        }

        // Phase 3: Tagline
        val taglineAlpha = ObjectAnimator.ofFloat(binding.tagline, View.ALPHA, 0f, 1f).apply {
            duration = 450; startDelay = 650
        }
        val taglineY = ObjectAnimator.ofFloat(binding.tagline, View.TRANSLATION_Y, 20f, 0f).apply {
            duration = 450; startDelay = 650; interpolator = DecelerateInterpolator()
        }

        // Phase 4: Progress + version
        val progressAlpha = ObjectAnimator.ofFloat(binding.progress, View.ALPHA, 0f, 1f).apply {
            duration = 300; startDelay = 900
        }
        val versionAlpha = ObjectAnimator.ofFloat(binding.tvVersion, View.ALPHA, 0f, 0.4f).apply {
            duration = 300; startDelay = 1000
        }

        val mainSet = AnimatorSet()
        mainSet.playTogether(
            logoScaleX, logoScaleY, logoAlpha, logoRotate,
            glowAlpha, glowScale, glowScaleY,
            titleAlpha, titleY,
            taglineAlpha, taglineY,
            progressAlpha, versionAlpha
        )

        mainSet.doOnEnd {
            startFloatingIconsAnimation()
            binding.root.postDelayed({ navigateToMain() }, 1600)
        }

        mainSet.start()
    }

    private fun startFloatingIconsAnimation() {
        // Each icon: (view, deltaX in dp, deltaY in dp, startDelay ms)
        val icons = listOf(
            FloatTarget(binding.iconDumbbell,  -110f, -130f,  0L),
            FloatTarget(binding.iconBarbell,    95f, -115f,  120L),
            FloatTarget(binding.iconNutrition, -70f, -160f,  60L),
            FloatTarget(binding.iconKettlebell, 115f, -145f, 180L)
        )

        val density = resources.displayMetrics.density

        icons.forEach { target ->
            val dxPx = target.dx * density
            val dyPx = target.dy * density

            val tx = ObjectAnimator.ofFloat(target.view, View.TRANSLATION_X, 0f, dxPx).apply {
                duration = 1400
                startDelay = target.delay
                interpolator = DecelerateInterpolator(1.5f)
            }
            val ty = ObjectAnimator.ofFloat(target.view, View.TRANSLATION_Y, 0f, dyPx).apply {
                duration = 1400
                startDelay = target.delay
                interpolator = DecelerateInterpolator(1.5f)
            }
            // Scale: grow from 0.6 → 1.1, then shrink to 0.7
            val scaleX = ObjectAnimator.ofFloat(target.view, View.SCALE_X, 0.6f, 1.1f, 0.7f).apply {
                duration = 1400; startDelay = target.delay
            }
            val scaleY = ObjectAnimator.ofFloat(target.view, View.SCALE_Y, 0.6f, 1.1f, 0.7f).apply {
                duration = 1400; startDelay = target.delay
            }
            // Alpha: appear → peak → fade out
            val alpha = ObjectAnimator.ofFloat(target.view, View.ALPHA, 0f, 0.85f, 0f).apply {
                duration = 1400; startDelay = target.delay
            }

            AnimatorSet().apply {
                playTogether(tx, ty, scaleX, scaleY, alpha)
                start()
            }
        }
    }

    private data class FloatTarget(val view: View, val dx: Float, val dy: Float, val delay: Long)

    private fun navigateToMain() {
        binding.root.animate()
            .alpha(0f)
            .scaleX(1.06f)
            .scaleY(1.06f)
            .setDuration(420)
            .withEndAction {
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                finish()
            }
            .start()
    }
}
