package com.fitnesstrainer.app.ui.trainer

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnRepeat

class ScannerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#BB000000")
    }
    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        shader = LinearGradient(0f, 0f, 200f, 200f,
            Color.parseColor("#5B4FE9"), Color.parseColor("#06B6D4"),
            Shader.TileMode.CLAMP)
    }
    private val scanLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * resources.displayMetrics.density
        color = Color.parseColor("#335B4FE9")
    }

    private val viewfinderRect = RectF()
    private var scanLineY = 0f
    private val cornerLen get() = viewfinderRect.width() * 0.12f
    private val cornerRadius = 6f * resources.displayMetrics.density

    private var scanAnimator: ValueAnimator? = null

    var scanSuccess = false
        set(value) {
            field = value
            if (value) pulseCorners()
        }

    private var cornerAlpha = 255
    private var cornerScale = 1f

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val size = (minOf(w, h) * 0.72f)
        val cx = w / 2f
        val cy = h / 2f - h * 0.04f
        viewfinderRect.set(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2)
        startScanLineAnimation()
    }

    private fun startScanLineAnimation() {
        scanAnimator?.cancel()
        scanAnimator = ValueAnimator.ofFloat(viewfinderRect.top, viewfinderRect.bottom).apply {
            duration = 2200
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener {
                scanLineY = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun pulseCorners() {
        ValueAnimator.ofFloat(1f, 1.08f, 1f).apply {
            duration = 350
            repeatCount = 2
            addUpdateListener {
                cornerScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        // dark overlay с вырезанным окном
        canvas.drawRect(0f, 0f, w, h, overlayPaint)
        canvas.drawRoundRect(viewfinderRect, cornerRadius, cornerRadius, clearPaint)

        // тонкая граница окна
        canvas.drawRoundRect(viewfinderRect, cornerRadius, cornerRadius, borderPaint)

        // угловые скобки
        val cl = cornerLen * cornerScale
        val r = viewfinderRect
        cornerPaint.alpha = cornerAlpha
        // top-left
        canvas.drawLine(r.left, r.top + cl, r.left, r.top + cornerRadius, cornerPaint)
        canvas.drawLine(r.left + cornerRadius, r.top, r.left + cl, r.top, cornerPaint)
        // top-right
        canvas.drawLine(r.right - cl, r.top, r.right - cornerRadius, r.top, cornerPaint)
        canvas.drawLine(r.right, r.top + cornerRadius, r.right, r.top + cl, cornerPaint)
        // bottom-left
        canvas.drawLine(r.left, r.bottom - cl, r.left, r.bottom - cornerRadius, cornerPaint)
        canvas.drawLine(r.left + cornerRadius, r.bottom, r.left + cl, r.bottom, cornerPaint)
        // bottom-right
        canvas.drawLine(r.right - cl, r.bottom, r.right - cornerRadius, r.bottom, cornerPaint)
        canvas.drawLine(r.right, r.bottom - cornerRadius, r.right, r.bottom - cl, cornerPaint)

        // анимированная линия сканирования
        if (!scanSuccess && scanLineY > viewfinderRect.top && scanLineY < viewfinderRect.bottom) {
            val lineH = 3f * resources.displayMetrics.density
            val progress = (scanLineY - viewfinderRect.top) / viewfinderRect.height()
            val alpha = (sinNorm(progress) * 220).toInt().coerceIn(80, 220)
            scanLinePaint.shader = LinearGradient(
                r.left, scanLineY, r.right, scanLineY,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.parseColor("#885B4FE9"),
                    Color.parseColor("#CC5B4FE9"),
                    Color.parseColor("#885B4FE9"),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.2f, 0.5f, 0.8f, 1f),
                Shader.TileMode.CLAMP
            )
            scanLinePaint.alpha = alpha
            canvas.drawRect(r.left, scanLineY - lineH / 2, r.right, scanLineY + lineH / 2, scanLinePaint)
        }
    }

    private fun sinNorm(x: Float): Float = (Math.sin(Math.PI * x.toDouble())).toFloat().coerceIn(0f, 1f)

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scanAnimator?.cancel()
    }
}
