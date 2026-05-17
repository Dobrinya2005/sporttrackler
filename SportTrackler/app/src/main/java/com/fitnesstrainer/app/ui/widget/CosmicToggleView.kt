package com.fitnesstrainer.app.ui.widget

import android.animation.*
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import kotlin.math.*
import kotlin.random.Random

class CosmicToggleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val d = resources.displayMetrics.density

    private val W = 140 * d
    private val H = 70  * d
    private val orbSize    = 62 * d
    private val innerInset = 5  * d

    var isChecked = false
        private set
    var onCheckedChanged: ((Boolean) -> Unit)? = null

    // Toggle fraction 0=off, 1=on
    private var frac = 0f

    // Stars
    private data class Star(val x: Float, val y: Float, val r: Float)
    private val stars = List(20) { Star(Random.nextFloat() * 140 * d, Random.nextFloat() * 70 * d, (0.5f + Random.nextFloat()) * d) }

    // Particles: 6 particles, each with angle, progress, alpha
    private val particleAngles  = floatArrayOf(30f, 60f, 90f, 120f, 150f, 180f).map { it * PI.toFloat() / 180f }.toFloatArray()
    private val particleOffsets = floatArrayOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1.0f)
    private val particleXs      = floatArrayOf(0.20f, 0.40f, 0.60f, 0.80f, 0.30f, 0.70f)
    private val particleProgress = FloatArray(6)

    // Energy lines: 3 lines, each has a phase offset
    private var energyPhase = 0f

    // Ring pulse scale & alpha
    private var ringScale = 1f; private var ringAlpha = 0.3f

    // Pattern rotation
    private var patternAngle = 0f

    // Paints
    private val bgPaint     = Paint(Paint.ANTI_ALIAS_FLAG)
    private val orbPaint    = Paint(Paint.ANTI_ALIAS_FLAG)
    private val innerPaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2 * d }
    private val starPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val energyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2 * d }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(130, 0, 0, 0)
        maskFilter = BlurMaskFilter(20 * d, BlurMaskFilter.Blur.NORMAL)
    }
    private val patternPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1 * d
        color = Color.argb(25, 0, 0, 0)
    }

    // Main toggle animator
    private val toggleAnim = ValueAnimator().apply {
        duration = 600
        interpolator = FastOutSlowInInterpolator()
        addUpdateListener { frac = it.animatedValue as Float; invalidate() }
    }

    // Continuous animation loop
    private val loopAnim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2000; repeatCount = ValueAnimator.INFINITE; interpolator = null
        addUpdateListener { v ->
            val t = v.animatedValue as Float
            energyPhase = t
            patternAngle = t * 360f
            // ring pulse
            ringScale = 1f + sin(t * 2 * PI.toFloat()) * 0.05f
            ringAlpha = 0.3f + sin(t * 2 * PI.toFloat()) * 0.15f
            // particles
            for (i in 0..5) {
                particleProgress[i] = ((t + particleOffsets[i]) % 1f)
            }
            invalidate()
        }
    }

    init { loopAnim.start() }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        setMeasuredDimension((W + 4 * d).toInt(), (H + 4 * d).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(2 * d, 2 * d)

        drawShadow(canvas)
        drawTrack(canvas)
        drawStars(canvas)
        drawEnergyLines(canvas)
        drawParticles(canvas)
        drawOrb(canvas)

        canvas.restore()
    }

    private val trackRect = RectF(0f, 0f, 0f, 0f)
    private fun drawShadow(canvas: Canvas) {
        trackRect.set(0f, 2 * d, W, H + 2 * d)
        canvas.drawRoundRect(trackRect, H / 2f, H / 2f, shadowPaint)
    }

    private fun drawTrack(canvas: Canvas) {
        trackRect.set(0f, 0f, W, H)
        // blend dark blue shades
        val c1 = Color.parseColor("#1a1a2e")
        val c2 = Color.parseColor("#16213e")
        bgPaint.shader = LinearGradient(0f, 0f, W, H, c1, c2, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(trackRect, H / 2f, H / 2f, bgPaint)

        // inner glow border
        bgPaint.shader = null
        bgPaint.color  = Color.argb(13, 255, 255, 255)
        bgPaint.style  = Paint.Style.STROKE; bgPaint.strokeWidth = 1 * d
        canvas.drawRoundRect(trackRect, H / 2f, H / 2f, bgPaint)
        bgPaint.style = Paint.Style.FILL
    }

    private fun drawStars(canvas: Canvas) {
        for (s in stars) {
            val a = (0.05f + 0.1f * frac) + sin(energyPhase * 2 * PI.toFloat() + s.x) * 0.05f
            starPaint.alpha = (a * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(s.x, s.y, s.r, starPaint)
        }
    }

    private fun drawEnergyLines(canvas: Canvas) {
        if (frac < 0.05f) return
        val alpha = (frac * 200).toInt()
        val tops = floatArrayOf(H * 0.20f, H * 0.50f, H * 0.80f)
        val angles = floatArrayOf(15f, 0f, -15f)

        for (i in 0..2) {
            val phase = (energyPhase + i * 0.33f) % 1f
            val scaleX = sin(phase * PI.toFloat()).coerceIn(0f, 1f)
            val offsetX = phase * W

            val gradient = LinearGradient(
                offsetX - W * scaleX * 0.5f, tops[i],
                offsetX + W * scaleX * 0.5f, tops[i],
                Color.TRANSPARENT,
                Color.argb(alpha, 78, 205, 196),
                Shader.TileMode.CLAMP
            )
            energyPaint.shader = gradient

            canvas.save()
            canvas.rotate(angles[i], W / 2f, tops[i])
            canvas.drawLine(0f, tops[i], W, tops[i], energyPaint)
            canvas.restore()
        }
        energyPaint.shader = null
    }

    private fun drawParticles(canvas: Canvas) {
        if (frac < 0.1f) return
        val alpha = frac
        val orbCx = 4 * d + orbSize / 2f + frac * (W - orbSize - 8 * d)
        val orbCy = H / 2f

        for (i in 0..5) {
            val p = particleProgress[i]
            val angle = particleAngles[i]
            val dist  = p * 50 * d
            val px    = orbCx + particleXs[i] * 20 * d - 10 * d + cos(angle) * dist
            val py    = orbCy + sin(angle) * dist
            particlePaint.color = Color.argb(
                ((1f - p) * alpha * 255).toInt().coerceIn(0, 255),
                78, 205, 196
            )
            val r = (1f - p) * 2 * d
            canvas.drawCircle(px, py, max(r, 0.5f * d), particlePaint)
        }
    }

    private fun drawOrb(canvas: Canvas) {
        val cx = 4 * d + orbSize / 2f + frac * (W - orbSize - 8 * d)
        val cy = H / 2f

        // orb glow
        if (frac > 0.1f) {
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb((frac * 80).toInt(), 78, 205, 196)
                maskFilter = BlurMaskFilter(20 * d, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawCircle(cx, cy, orbSize / 2f + 5 * d, glowPaint)
        }

        // orb gradient: off = #ff6b6b→#4ecdc4, on = #4ecdc4→#45b7af
        val c1off = Color.parseColor("#ff6b6b"); val c2off = Color.parseColor("#4ecdc4")
        val c1on  = Color.parseColor("#4ecdc4"); val c2on  = Color.parseColor("#45b7af")
        orbPaint.shader = LinearGradient(
            cx - orbSize / 2f, cy - orbSize / 2f,
            cx + orbSize / 2f, cy + orbSize / 2f,
            blendColors(c1off, c1on, frac), blendColors(c2off, c2on, frac),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, orbSize / 2f, orbPaint)

        // inner orb
        val innerR = orbSize / 2f - innerInset
        val i1off = Color.parseColor("#ffffff"); val i1on = Color.parseColor("#45b7af")
        val i2off = Color.parseColor("#e6e6e6"); val i2on = Color.parseColor("#3da89f")
        innerPaint.shader = LinearGradient(
            cx - innerR, cy - innerR, cx + innerR, cy + innerR,
            blendColors(i1off, i1on, frac), blendColors(i2off, i2on, frac),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, innerR, innerPaint)

        // rotating pattern on inner orb
        canvas.save()
        canvas.clipRect(cx - innerR, cy - innerR, cx + innerR, cy + innerR)
        canvas.rotate(patternAngle, cx, cy)
        var a = 0f
        while (a < 360f) {
            val rad = a * PI.toFloat() / 180f
            canvas.drawLine(cx, cy, cx + cos(rad) * innerR * 2, cy + sin(rad) * innerR * 2, patternPaint)
            a += 20f
        }
        canvas.restore()

        // ring
        ringPaint.color = if (frac > 0.5f)
            Color.argb((ringAlpha * 255).toInt(), 78, 205, 196)
        else
            Color.argb(25, 255, 255, 255)
        canvas.drawCircle(cx, cy, orbSize / 2f + 3 * d, ringPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            isChecked = !isChecked
            toggleAnim.cancel()
            toggleAnim.setFloatValues(frac, if (isChecked) 1f else 0f)
            toggleAnim.start()
            onCheckedChanged?.invoke(isChecked)
        }
        return true
    }

    fun setChecked(checked: Boolean, animate: Boolean = false) {
        if (isChecked == checked) return
        isChecked = checked
        if (animate) {
            toggleAnim.cancel()
            toggleAnim.setFloatValues(frac, if (checked) 1f else 0f)
            toggleAnim.start()
        } else {
            frac = if (checked) 1f else 0f
            invalidate()
        }
    }

    private fun blendColors(c1: Int, c2: Int, f: Float): Int {
        val t = f.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(c1)   + (Color.red(c2)   - Color.red(c1))   * t).toInt(),
            (Color.green(c1) + (Color.green(c2) - Color.green(c1)) * t).toInt(),
            (Color.blue(c1)  + (Color.blue(c2)  - Color.blue(c1))  * t).toInt()
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        loopAnim.cancel(); toggleAnim.cancel()
    }
}
