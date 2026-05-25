package com.fitnesstrainer.app.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class LoginButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val d = resources.displayMetrics.density

    private val W = 131 * d
    private val H = 51  * d
    private val outerR = 15 * d
    private val innerR = 13 * d
    private val innerInset = 2 * d

    private val blue       = Color.parseColor("#2e8eff")
    private val blueAlpha  = Color.argb(51,  46, 142, 255)   // 0.2 alpha
    private val blueHover  = Color.argb(179, 46, 142, 255)   // 0.7 alpha
    private val innerBg    = Color.parseColor("#1a1a1a")
    private val white      = Color.WHITE

    // hover fraction 0..1
    private var hoverFrac = 0f
    private val hoverAnim = ValueAnimator().apply {
        duration = 300
        addUpdateListener { hoverFrac = it.animatedValue as Float; invalidate() }
    }

    private val bgPaint   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val innerPaint= Paint(Paint.ANTI_ALIAS_FLAG).apply { color = innerBg }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        textSize = 15 * d
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white }

    // user-profile icon path (scaled from viewBox 0 0 24 24)
    private val iconPath = Path()
    private var iconMatrix = Matrix()

    var onClick: (() -> Unit)? = null

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        setMeasuredDimension((W + 20 * d).toInt(), (H + 20 * d).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        buildIconPath()
    }

    private fun buildIconPath() {
        // path from SVG viewBox 24x24, will be transformed to 27x27dp
        iconPath.reset()
        // head circle approximation + body arc
        iconPath.addCircle(12f, 7f, 4f, Path.Direction.CW)
        val body = RectF(3f, 14f, 21f, 23f)
        iconPath.arcTo(body, 180f, -180f)
        iconPath.close()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(10 * d, 10 * d)

        // glow shadow when hovered
        if (hoverFrac > 0f) {
            glowPaint.color = Color.argb((hoverFrac * 130).toInt(), 46, 142, 255)
            glowPaint.maskFilter = BlurMaskFilter(10 * d, BlurMaskFilter.Blur.NORMAL)
            canvas.drawRoundRect(RectF(0f, 0f, W, H), outerR, outerR, glowPaint)
        }

        // outer bg: gradient overlay + flat color
        val flatColor = blendColors(blueAlpha, blueHover, hoverFrac)
        bgPaint.color = flatColor
        bgPaint.shader = null
        canvas.drawRoundRect(RectF(0f, 0f, W, H), outerR, outerR, bgPaint)

        // gradient top-left corner
        bgPaint.shader = LinearGradient(
            0f, 0f, W * 0.5f, H * 0.5f,
            Color.argb((255 * (0.3f + hoverFrac * 0.4f)).toInt(), 46, 142, 255),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(RectF(0f, 0f, W, H), outerR, outerR, bgPaint)
        bgPaint.shader = null

        // inner dark panel
        val inner = RectF(innerInset, innerInset, W - innerInset, H - innerInset)
        innerPaint.color = innerBg
        canvas.drawRoundRect(inner, innerR, innerR, innerPaint)

        // icon (27x27 dp) + text centered with gap 15dp
        val iconSize = 27 * d
        val text     = "Войти"
        val textW    = textPaint.measureText(text)
        val gap      = 15 * d
        val contentW = iconSize + gap + textW
        val startX   = (W - contentW) / 2f
        val centerY  = H / 2f

        drawUserIcon(canvas, startX, centerY - iconSize / 2f, iconSize)

        val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(text, startX + iconSize + gap, textY, textPaint)

        canvas.restore()
    }

    private fun drawUserIcon(canvas: Canvas, x: Float, y: Float, size: Float) {
        val scale = size / 24f
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(scale, scale)

        // head
        iconPaint.style = Paint.Style.FILL
        canvas.drawCircle(12f, 7f, 4f, iconPaint)

        // body: arc segment bottom
        val bodyPath = Path()
        bodyPath.moveTo(1f, 23f)
        bodyPath.rCubicTo(0f, -6f, 4f, -11f, 11f, -11f)
        bodyPath.rCubicTo(7f, 0f, 11f, 5f, 11f, 11f)
        bodyPath.close()
        canvas.drawPath(bodyPath, iconPaint)

        canvas.restore()
    }

    private fun animateTo(hover: Boolean) {
        hoverAnim.cancel()
        hoverAnim.setFloatValues(hoverFrac, if (hover) 1f else 0f)
        hoverAnim.start()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN  -> animateTo(true)
            MotionEvent.ACTION_UP    -> {
                animateTo(false)
                val inBounds = event.x in 0f..(W + 20 * d) && event.y in 0f..(H + 20 * d)
                if (inBounds) onClick?.invoke()
            }
            MotionEvent.ACTION_CANCEL -> animateTo(false)
        }
        return true
    }

    private fun blendColors(c1: Int, c2: Int, f: Float): Int {
        val t = f.coerceIn(0f, 1f)
        return Color.argb(
            (Color.alpha(c1) + (Color.alpha(c2) - Color.alpha(c1)) * t).toInt(),
            (Color.red(c1)   + (Color.red(c2)   - Color.red(c1))   * t).toInt(),
            (Color.green(c1) + (Color.green(c2) - Color.green(c1)) * t).toInt(),
            (Color.blue(c1)  + (Color.blue(c2)  - Color.blue(c1))  * t).toInt()
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        hoverAnim.cancel()
    }
}
