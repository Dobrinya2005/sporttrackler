package com.fitnesstrainer.app.ui.chat

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var amplitudes: FloatArray = floatArrayOf()
        set(value) { field = value; invalidate() }

    var progress: Float = 0f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    private val dp = context.resources.displayMetrics.density
    private val barWidth = dp * 3f
    private val barGap   = dp * 1.5f
    private val minFrac  = 0.12f

    private val paintPlayed = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6C8EBF.toInt() }
    private val paintUnplayed = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x556C8EBF.toInt() }
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bars = amplitudes
        if (bars.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val step = barWidth + barGap
        val count = (w / step).toInt().coerceAtLeast(1)
        val playedIdx = (progress * count).toInt()

        for (i in 0 until count) {
            val amp = if (bars.isNotEmpty()) bars[i % bars.size] else 0.5f
            val barH = (amp.coerceIn(minFrac, 1f) * h * 0.88f).coerceAtLeast(h * minFrac)
            val left = i * step
            val top  = (h - barH) / 2f
            rect.set(left, top, left + barWidth, top + barH)
            val paint = if (i < playedIdx) paintPlayed else paintUnplayed
            canvas.drawRoundRect(rect, barWidth / 2, barWidth / 2, paint)
        }
    }

    companion object {
        fun pseudoWaveform(seed: Int, count: Int = 60): FloatArray {
            val rng = java.util.Random(seed.toLong())
            return FloatArray(count) { 0.15f + rng.nextFloat() * 0.85f }
        }
    }
}
