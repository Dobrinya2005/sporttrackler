package com.fitnesstrainer.app.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.VectorDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.fitnesstrainer.app.R
import kotlin.math.min

class LogoutButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val d = resources.displayMetrics.density

    private val collapsedW = (45 * d)
    private val expandedW  = (125 * d)
    private val buttonH    = (45 * d)

    private var currentW   = collapsedW
    private var textAlpha  = 0f
    private var pressShift = 0f

    private val bgColor = Color.rgb(255, 65, 65)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(50, 0, 0, 0)
        maskFilter = BlurMaskFilter(12 * d, BlurMaskFilter.Blur.NORMAL)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 15 * d
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
    }

    private val icon: Drawable? by lazy {
        ContextCompat.getDrawable(context, R.drawable.ic_logout)
    }

    private val expandAnim = ValueAnimator().apply {
        duration = 300
        interpolator = FastOutSlowInInterpolator()
        addUpdateListener {
            val frac = it.animatedValue as Float
            currentW  = collapsedW + (expandedW  - collapsedW)  * frac
            textAlpha = frac
            requestLayout(); invalidate()
        }
    }

    private val pressAnim = ValueAnimator().apply {
        duration = 100
        addUpdateListener { pressShift = it.animatedValue as Float; invalidate() }
    }

    var onClick: (() -> Unit)? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(expandedW.toInt() + (12 * d).toInt(), (buttonH + 12 * d).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(pressShift, pressShift)

        val radius = currentW / 2f
        val rect = RectF(6 * d, 6 * d, 6 * d + currentW, 6 * d + buttonH)

        // shadow
        canvas.drawRoundRect(rect.apply { offset(1 * d, 1 * d) }, radius, radius, shadowPaint)
        rect.offset(-1 * d, -1 * d)

        // background
        canvas.drawRoundRect(rect, radius, radius, bgPaint)

        // icon — centered in the left 30% when expanded, or full width when collapsed
        val iconZone = if (textAlpha > 0f) currentW * 0.30f else currentW
        val iconSize = (17 * d).toInt()
        val iconX = (rect.left + (iconZone - iconSize) / 2f).toInt()
        val iconY = (rect.top + (buttonH - iconSize) / 2f).toInt()
        icon?.setBounds(iconX, iconY, iconX + iconSize, iconY + iconSize)
        icon?.alpha = 255
        icon?.draw(canvas)

        // text
        if (textAlpha > 0f) {
            textPaint.alpha = (textAlpha * 255).toInt()
            val textY = rect.top + buttonH / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText("Logout", rect.right - 10 * d, textY, textPaint)
        }

        canvas.restore()
    }

    private fun animateTo(expanded: Boolean) {
        expandAnim.cancel()
        expandAnim.setFloatValues(textAlpha, if (expanded) 1f else 0f)
        expandAnim.start()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                animateTo(true)
                pressAnim.cancel(); pressAnim.setFloatValues(pressShift, 2 * d); pressAnim.start()
            }
            MotionEvent.ACTION_UP -> {
                pressAnim.cancel(); pressAnim.setFloatValues(pressShift, 0f); pressAnim.start()
                if (event.x in 0f..currentW + 12 * d && event.y in 0f..buttonH + 12 * d) {
                    onClick?.invoke()
                }
                animateTo(false)
            }
            MotionEvent.ACTION_CANCEL -> {
                pressAnim.cancel(); pressAnim.setFloatValues(pressShift, 0f); pressAnim.start()
                animateTo(false)
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        expandAnim.cancel(); pressAnim.cancel()
    }
}

private typealias Drawable = android.graphics.drawable.Drawable
