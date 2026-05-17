package com.fitnesstrainer.app.ui.widget

import android.animation.*
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.fitnesstrainer.app.R
import kotlin.math.*

class SendMessageButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val d = resources.displayMetrics.density

    // Colors
    private val primary   = Color.parseColor("#ff5569")
    private val neutral1  = Color.parseColor("#f7f8f7")
    private val neutral2  = Color.parseColor("#e7e7e7")

    // Dimensions
    private val btnW = (200 * d); private val btnH = (68 * d)
    private val radius = 14 * d

    // State: 0=default, 1=sending (takeoff), 2=sent
    private var state = 0

    // Letter wave: each letter has a Y offset and alpha
    private val label = "Send Message"
    private val sentLabel = "Sent"
    private val letterOffsets = FloatArray(label.length)
    private val letterAlphas  = FloatArray(label.length) { 1f }
    private val sentAlphas    = FloatArray(sentLabel.length)
    private val sentOffsets   = FloatArray(sentLabel.length) { -20 * d }

    // Plane position & rotation (for takeoff)
    private var planeX = 0f; private var planeY = 0f
    private var planeRotation = 0f; private var planeScale = 1f; private var planeAlpha = 255

    // Check icon alpha & scale
    private var checkAlpha = 0; private var checkScale = 0f

    // Press scale
    private var btnScale = 1f

    // Spin outline angle
    private var outlineAngle = 0f
    private var outlineAlpha = 0f
    private val spinAnim = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 2000; repeatCount = ValueAnimator.INFINITE; interpolator = null
        addUpdateListener { outlineAngle = it.animatedValue as Float; invalidate() }
    }

    // Paints
    private val bgPaint     = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.5f * d }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(50, 0, 0, 0)
        maskFilter = BlurMaskFilter(16 * d, BlurMaskFilter.Blur.NORMAL)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        textSize = 18 * d
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2f * d
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#333333") }

    var onClick: (() -> Unit)? = null

    override fun onMeasure(w: Int, h: Int) {
        setMeasuredDimension((btnW + 20 * d).toInt(), (btnH + 20 * d).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        resetPlane()
    }

    private fun resetPlane() {
        planeX = 29 * d; planeY = btnH / 2f + 10 * d
        planeRotation = 0f; planeScale = 1.25f; planeAlpha = 255
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(10 * d, 10 * d)
        canvas.scale(btnScale, btnScale, btnW / 2f, btnH / 2f)

        drawShadow(canvas)
        drawBackground(canvas)
        drawBorder(canvas)
        drawOutline(canvas)
        drawInnerHighlight(canvas)

        when (state) {
            0, 1 -> { drawPlane(canvas); drawLetters(canvas, label, letterOffsets, letterAlphas) }
            2    -> { drawCheckIcon(canvas); drawLetters(canvas, sentLabel, sentOffsets, sentAlphas, forSent = true) }
        }

        canvas.restore()
    }

    private val rect = RectF()
    private fun drawShadow(canvas: Canvas) {
        rect.set(2 * d, 4 * d, btnW - 2 * d, btnH + 2 * d)
        canvas.drawRoundRect(rect, radius, radius, shadowPaint)
    }

    private fun drawBackground(canvas: Canvas) {
        rect.set(0f, 0f, btnW, btnH)
        bgPaint.shader = LinearGradient(0f, 0f, 0f, btnH, neutral1, neutral2, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, radius, radius, bgPaint)
    }

    private fun drawBorder(canvas: Canvas) {
        rect.set(1.25f * d, 1.25f * d, btnW - 1.25f * d, btnH - 1.25f * d)
        borderPaint.shader = LinearGradient(0f, 0f, 0f, btnH,
            Color.argb(25, 0, 0, 0), Color.argb(115, 0, 0, 0), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, radius - 1.25f * d, radius - 1.25f * d, borderPaint)
    }

    private fun drawInnerHighlight(canvas: Canvas) {
        rect.set(6 * d, 7 * d, btnW - 6 * d, btnH - 6 * d)
        bgPaint.shader = LinearGradient(0f, 0f, 0f, btnH, neutral1, neutral2, Shader.TileMode.CLAMP)
        bgPaint.maskFilter = BlurMaskFilter(1 * d, BlurMaskFilter.Blur.NORMAL)
        canvas.drawRoundRect(rect, 30 * d, 30 * d, bgPaint)
        bgPaint.maskFilter = null
    }

    private fun drawOutline(canvas: Canvas) {
        if (outlineAlpha <= 0f) return
        val cx = btnW / 2f; val cy = btnH / 2f
        val r  = max(btnW, btnH) * 0.75f
        val sweep = SweepGradient(cx, cy,
            intArrayOf(Color.TRANSPARENT, Color.WHITE, Color.TRANSPARENT),
            floatArrayOf(0.6f, 0.8f, 1f))
        val m = Matrix(); m.setRotate(outlineAngle, cx, cy)
        sweep.setLocalMatrix(m)
        outlinePaint.shader = sweep
        outlinePaint.alpha  = (outlineAlpha * 255).toInt()
        rect.set(-3.5f * d, -2 * d, btnW + 3.5f * d, btnH + 2 * d)
        canvas.drawRoundRect(rect, radius + 2 * d, radius + 2 * d, outlinePaint)
    }

    // Draw send-plane icon using path
    private val planePath = Path()
    private fun drawPlane(canvas: Canvas) {
        canvas.save()
        canvas.translate(planeX, planeY)
        canvas.rotate(planeRotation)
        canvas.scale(planeScale, planeScale)
        iconPaint.alpha = planeAlpha

        // simple paper-plane shape
        planePath.reset()
        planePath.moveTo(-10 * d, -6 * d)
        planePath.lineTo(10 * d,  0f)
        planePath.lineTo(-10 * d,  6 * d)
        planePath.lineTo(-6  * d,  0f)
        planePath.close()
        canvas.drawPath(planePath, iconPaint)

        canvas.restore()

        // contrail during takeoff
        if (state == 1) {
            val contrailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(planeX - 30 * d, 0f, planeX, 0f,
                    Color.TRANSPARENT, Color.argb(120, 0, 0, 0), Shader.TileMode.CLAMP)
                strokeWidth = 2 * d; style = Paint.Style.STROKE
            }
            canvas.drawLine(planeX - 30 * d, planeY, planeX, planeY, contrailPaint)
        }
    }

    private fun drawCheckIcon(canvas: Canvas) {
        if (checkAlpha <= 0) return
        canvas.save()
        val cx = 29 * d; val cy = btnH / 2f
        canvas.scale(checkScale, checkScale, cx, cy)
        iconPaint.alpha = checkAlpha

        // circle
        iconPaint.style = Paint.Style.STROKE; iconPaint.strokeWidth = 1.5f * d
        canvas.drawCircle(cx, cy, 10 * d, iconPaint)

        // checkmark
        iconPaint.style = Paint.Style.STROKE; iconPaint.strokeWidth = 1.5f * d
        val ck = Path()
        ck.moveTo(cx - 5 * d, cy)
        ck.lineTo(cx - 1 * d, cy + 4 * d)
        ck.lineTo(cx + 6 * d, cy - 4 * d)
        canvas.drawPath(ck, iconPaint)
        iconPaint.style = Paint.Style.FILL

        canvas.restore()
    }

    private fun drawLetters(canvas: Canvas, text: String, offsets: FloatArray, alphas: FloatArray, forSent: Boolean = false) {
        val totalW = textPaint.measureText(text)
        val startX = if (forSent) (btnW - totalW) / 2f + 14 * d else 48 * d
        var x = startX
        text.forEachIndexed { i, ch ->
            textPaint.alpha = (alphas[i] * 255).toInt()
            val chW = textPaint.measureText(ch.toString())
            canvas.drawText(ch.toString(), x, btnH / 2f - (textPaint.descent() + textPaint.ascent()) / 2f + offsets[i], textPaint)
            x += chW
        }
        textPaint.alpha = 255
    }

    // ---- Animations ----

    private fun animateHoverIn() {
        outlineAlpha = 1f; if (!spinAnim.isRunning) spinAnim.start()
        label.indices.forEach { i ->
            ValueAnimator.ofFloat(letterOffsets[i], 4 * d, -3 * d, 0f).apply {
                duration = 500; startDelay = (i * 20).toLong()
                interpolator = FastOutSlowInInterpolator()
                addUpdateListener { letterOffsets[i] = it.animatedValue as Float; invalidate() }
            }.start()
            ValueAnimator.ofFloat(letterAlphas[i], 1f).apply {
                duration = 300; startDelay = (i * 20).toLong()
                addUpdateListener { letterAlphas[i] = it.animatedValue as Float }
            }.start()
        }
        ValueAnimator.ofFloat(btnScale, 1.02f).apply {
            duration = 150
            addUpdateListener { btnScale = it.animatedValue as Float; invalidate() }
        }.start()
    }

    private fun animateHoverOut() {
        outlineAlpha = 0f; spinAnim.cancel()
        ValueAnimator.ofFloat(btnScale, 1f).apply {
            duration = 150
            addUpdateListener { btnScale = it.animatedValue as Float; invalidate() }
        }.start()
    }

    private fun animateSend() {
        state = 1
        onClick?.invoke()

        // letters disappear
        label.indices.forEach { i ->
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 600; startDelay = (i * 30).toLong()
                addUpdateListener {
                    val f = it.animatedValue as Float
                    letterAlphas[i]  = 1f - f
                    letterOffsets[i] = f * 20 * d
                    invalidate()
                }
            }.start()
        }

        // plane takeoff
        val takeoff = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 800; startDelay = 100
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener {
                val f = it.animatedValue as Float
                planeX        = 29 * d + f * btnW * 0.9f
                planeRotation = f * 45f
                planeScale    = 1.25f + f * 0.75f
                planeAlpha    = ((1f - f) * 255).toInt()
                invalidate()
            }
        }
        takeoff.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) { transitionToSent() }
        })
        takeoff.start()
    }

    private fun transitionToSent() {
        state = 2
        checkAlpha = 0; checkScale = 4f
        sentAlphas.fill(0f); sentOffsets.fill(-20 * d)

        // check icon appears
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200; startDelay = 800
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener {
                val f = it.animatedValue as Float
                checkAlpha = (f * 255).toInt()
                checkScale = 4f - f * 3f
                invalidate()
            }
        }.start()

        // sent letters slide in
        sentLabel.indices.forEach { i ->
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 800; startDelay = (i * 200 + 200).toLong()
                interpolator = FastOutSlowInInterpolator()
                addUpdateListener {
                    val f = it.animatedValue as Float
                    sentAlphas[i]  = f
                    sentOffsets[i] = -20 * d + f * 20 * d
                    invalidate()
                }
            }.start()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                animateHoverIn()
                true
            }
            MotionEvent.ACTION_UP -> {
                animateHoverOut()
                if (state == 0 &&
                    event.x in 0f..(btnW + 20 * d) &&
                    event.y in 0f..(btnH + 20 * d)) {
                    animateSend()
                }
                true
            }
            MotionEvent.ACTION_CANCEL -> { animateHoverOut(); true }
            else -> false
        }
    }

    fun reset() {
        state = 0
        letterOffsets.fill(0f); letterAlphas.fill(1f)
        sentAlphas.fill(0f);    sentOffsets.fill(-20 * d)
        checkAlpha = 0; checkScale = 0f
        resetPlane(); btnScale = 1f; outlineAlpha = 0f
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        spinAnim.cancel()
    }
}
