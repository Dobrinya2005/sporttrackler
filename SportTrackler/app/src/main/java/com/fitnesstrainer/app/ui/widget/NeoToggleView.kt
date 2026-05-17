package com.fitnesstrainer.app.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import kotlin.math.*

class NeoToggleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Colors
    private val bgColor       = Color.parseColor("#181c20")
    private val offColor      = Color.parseColor("#475057")
    private val onColor       = Color.parseColor("#36f9c7")
    private val gridColor     = Color.parseColor("#0D475057")

    // Dimensions (dp → px in onSizeChanged)
    private var trackW = 0f; private var trackH = 0f
    private var thumbR = 0f; private var thumbY  = 0f
    private var thumbOffX = 0f; private var thumbOnX = 0f

    // State
    private var thumbX     = 0f
    private var toggleFrac = 0f   // 0=off, 1=on

    var isChecked = false
        private set

    var onCheckedChanged: ((Boolean) -> Unit)? = null

    // Animators
    private val toggleAnim = ValueAnimator().apply {
        duration = 400
        interpolator = FastOutSlowInInterpolator()
        addUpdateListener {
            toggleFrac = it.animatedValue as Float
            thumbX = thumbOffX + (thumbOnX - thumbOffX) * toggleFrac
            invalidate()
        }
    }

    // Spectrum bars: 5 bars, each with independent height fraction (0..1)
    private val specBars = FloatArray(5)
    private val specAnimators = Array(5) { i ->
        ValueAnimator.ofFloat(0f, 1f, 0f).apply {
            duration = (700 + i * 80).toLong()
            startDelay = (i * 60).toLong()
            repeatCount = ValueAnimator.INFINITE
            interpolator = DecelerateInterpolator()
            addUpdateListener { a -> specBars[i] = a.animatedValue as Float; invalidate() }
        }
    }

    // Ripple
    private var rippleAlpha = 0f
    private var rippleRadius = 0f
    private val rippleAnim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 500
        interpolator = DecelerateInterpolator()
        addUpdateListener {
            val f = it.animatedValue as Float
            rippleAlpha  = (1f - f) * 0.4f
            rippleRadius = f * trackW * 0.6f
            invalidate()
        }
    }

    // Pulse ring around thumb
    private var pulseScale = 0f; private var pulseAlpha = 0f
    private val pulseAnim = ValueAnimator.ofFloat(1f, 1.6f, 1f).apply {
        duration = 1500; repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
            pulseScale = it.animatedValue as Float
            pulseAlpha = 0.4f - 0.3f * abs(it.animatedFraction - 0.5f) * 2f
            invalidate()
        }
    }

    // Paints
    private val trackPaint   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbPaint   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint    = Paint(Paint.ANTI_ALIAS_FLAG)
    private val specPaint    = Paint(Paint.ANTI_ALIAS_FLAG)
    private val statusPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val ripplePaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint    = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gridColor; strokeWidth = 1f }
    private val pulsePaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val w = (80 * density).toInt()
        val h = (56 * density).toInt()   // extra height for status text
        setMeasuredDimension(
            resolveSize(w, widthMeasureSpec),
            resolveSize(h, heightMeasureSpec)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val d = resources.displayMetrics.density
        trackW    = w.toFloat()
        trackH    = 38 * d
        thumbR    = 15 * d
        thumbY    = trackH / 2f
        thumbOffX = 4 * d + thumbR
        thumbOnX  = trackW - 4 * d - thumbR
        thumbX    = if (isChecked) thumbOnX else thumbOffX
        statusPaint.textSize = 8 * d
    }

    override fun onDraw(canvas: Canvas) {
        drawTrack(canvas)
        drawGrid(canvas)
        drawRipple(canvas)
        drawSpectrumBars(canvas)
        drawPulse(canvas)
        drawThumb(canvas)
        drawStatusText(canvas)
    }

    private val trackRect = RectF()
    private fun drawTrack(canvas: Canvas) {
        val r = trackH / 2f
        trackRect.set(0f, 0f, trackW, trackH)

        // shadow
        glowPaint.apply {
            color = if (isChecked) Color.argb((60 * toggleFrac).toInt(), 54, 249, 199) else Color.TRANSPARENT
            maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(trackRect, r, r, glowPaint)

        // base bg
        trackPaint.color = bgColor
        canvas.drawRoundRect(trackRect, r, r, trackPaint)

        // tint overlay when on
        if (toggleFrac > 0f) {
            trackPaint.color = Color.argb((30 * toggleFrac).toInt(), 54, 249, 199)
            canvas.drawRoundRect(trackRect, r, r, trackPaint)
        }

        // border
        trackPaint.apply { color = Color.argb(25, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = 1f }
        canvas.drawRoundRect(trackRect, r, r, trackPaint)
        trackPaint.style = Paint.Style.FILL
    }

    private fun drawGrid(canvas: Canvas) {
        if (toggleFrac < 0.01f) return
        val alpha = (toggleFrac * 40).toInt()
        gridPaint.alpha = alpha
        val step = 5 * resources.displayMetrics.density
        var x = step
        while (x < trackW) { canvas.drawLine(x, 0f, x, trackH, gridPaint); x += step }
        var y = step
        while (y < trackH) { canvas.drawLine(0f, y, trackW, y, gridPaint); y += step }
    }

    private fun drawRipple(canvas: Canvas) {
        if (rippleAlpha <= 0f) return
        ripplePaint.apply {
            shader = RadialGradient(thumbX, thumbY, rippleRadius,
                Color.argb((rippleAlpha * 255).toInt(), 54, 249, 199),
                Color.TRANSPARENT, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(thumbX, thumbY, rippleRadius, ripplePaint)
    }

    private fun drawSpectrumBars(canvas: Canvas) {
        if (toggleFrac < 0.01f) return
        val d = resources.displayMetrics.density
        val barW = 2 * d; val maxH = 10 * d; val minH = 3 * d
        val startX = trackW - 10 * d - 5 * (barW + 2 * d)
        specPaint.color = Color.argb((toggleFrac * 200).toInt(), 54, 249, 199)
        for (i in 0..4) {
            val bh = minH + (maxH - minH) * specBars[i]
            val x = startX + i * (barW + 2 * d)
            canvas.drawRect(x, trackH - 6 * d - bh, x + barW, trackH - 6 * d, specPaint)
        }
    }

    private fun drawPulse(canvas: Canvas) {
        if (!isChecked || pulseAlpha <= 0f) return
        pulsePaint.color = Color.argb((pulseAlpha * 255).toInt(), 54, 249, 199)
        canvas.drawCircle(thumbX, thumbY, thumbR * pulseScale, pulsePaint)
    }

    private fun drawThumb(canvas: Canvas) {
        // glow
        if (toggleFrac > 0f) {
            glowPaint.apply {
                color = Color.argb((120 * toggleFrac).toInt(), 54, 249, 199)
                maskFilter = BlurMaskFilter(thumbR * 0.8f, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawCircle(thumbX, thumbY, thumbR, glowPaint)
        }

        // thumb fill
        val thumbColor = blendColors(offColor, onColor, toggleFrac)
        thumbPaint.color = thumbColor
        thumbPaint.maskFilter = null
        canvas.drawCircle(thumbX, thumbY, thumbR, thumbPaint)

        // inner highlight
        thumbPaint.color = Color.argb(25, 255, 255, 255)
        canvas.drawCircle(thumbX - thumbR * 0.15f, thumbY - thumbR * 0.2f, thumbR * 0.6f, thumbPaint)
    }

    private fun drawStatusText(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val label = if (isChecked) "ACTIVE" else "STANDBY"
        val color = blendColors(offColor, onColor, toggleFrac)
        statusPaint.color = color
        canvas.drawText(label, trackW / 2f, trackH + 14 * d, statusPaint)

        // dot
        val dotR = 3 * d
        val dotX = trackW / 2f - statusPaint.measureText(label) / 2f - 8 * d
        val dotY  = trackH + 10 * d
        thumbPaint.color = color
        canvas.drawCircle(dotX, dotY, dotR, thumbPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            toggle()
            return true
        }
        return true
    }

    fun toggle() {
        isChecked = !isChecked
        toggleAnim.cancel()
        toggleAnim.setFloatValues(toggleFrac, if (isChecked) 1f else 0f)
        toggleAnim.start()
        rippleAnim.cancel(); rippleAnim.start()
        if (isChecked) {
            specAnimators.forEach { it.start() }
            pulseAnim.start()
        } else {
            specAnimators.forEach { it.cancel(); }
            pulseAnim.cancel(); pulseScale = 0f; pulseAlpha = 0f
        }
        onCheckedChanged?.invoke(isChecked)
    }

    fun setChecked(checked: Boolean, animate: Boolean = false) {
        if (isChecked == checked) return
        if (animate) { toggle() } else {
            isChecked = checked
            toggleFrac = if (checked) 1f else 0f
            thumbX = if (checked) thumbOnX else thumbOffX
            if (checked) { specAnimators.forEach { it.start() }; pulseAnim.start() }
            else { specAnimators.forEach { it.cancel() }; pulseAnim.cancel() }
            invalidate()
        }
    }

    private fun blendColors(c1: Int, c2: Int, fraction: Float): Int {
        val f = fraction.coerceIn(0f, 1f)
        val r = (Color.red(c1)   + (Color.red(c2)   - Color.red(c1))   * f).toInt()
        val g = (Color.green(c1) + (Color.green(c2) - Color.green(c1)) * f).toInt()
        val b = (Color.blue(c1)  + (Color.blue(c2)  - Color.blue(c1))  * f).toInt()
        return Color.rgb(r, g, b)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        toggleAnim.cancel(); rippleAnim.cancel(); pulseAnim.cancel()
        specAnimators.forEach { it.cancel() }
    }
}
