package com.fitnesstrainer.app.ui.widget

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast

class MessageBoxView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val d = resources.displayMetrics.density

    private val bgColor     = Color.parseColor("#2d2d2d")
    private val borderIdle  = Color.parseColor("#3f3f3f")
    private val borderFocus = Color.parseColor("#6e6e6e")
    private val iconIdle    = Color.parseColor("#6c6c6c")
    private val iconHover   = Color.WHITE
    private val iconHoverBg = Color.parseColor("#3c3c3c")

    // Public callbacks
    var onSendClick: ((String) -> Unit)? = null
    var onAttachClick: (() -> Unit)?     = null

    private val editText: EditText
    private val attachBtn: AttachIconView
    private val sendBtn: SendIconView

    // Border color animator
    private var borderColor = borderIdle
    private val borderAnim = ValueAnimator().apply {
        duration = 200
        addUpdateListener { borderColor = it.animatedValue as Int; invalidateSelf() }
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding((15 * d).toInt(), 0, (15 * d).toInt(), 0)
        setBackgroundColor(Color.TRANSPARENT)
        setWillNotDraw(false)

        // Attach button
        attachBtn = AttachIconView(context).apply {
            layoutParams = LayoutParams((28 * d).toInt(), (40 * d).toInt())
            onHoverColor  = iconHover
            onHoverBgColor = iconHoverBg
            idleColor     = iconIdle
            this.onAttachClick = { this@MessageBoxView.onAttachClick?.invoke() }
        }
        addView(attachBtn)

        // Text input
        editText = EditText(context).apply {
            layoutParams = LayoutParams(0, (40 * d).toInt(), 1f)
            hint = "Message..."
            setHintTextColor(Color.parseColor("#888888"))
            setTextColor(Color.WHITE)
            textSize = 14f
            background = null
            setPadding((10 * d).toInt(), 0, 0, 0)
            setSingleLine(true)
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    sendBtn.hasText = !s.isNullOrEmpty()
                }
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            })
            setOnFocusChangeListener { _, focused ->
                animateBorder(focused)
                sendBtn.hasText = focused || !text.isNullOrEmpty()
            }
        }
        addView(editText)

        // Send button
        sendBtn = SendIconView(context).apply {
            layoutParams = LayoutParams((28 * d).toInt(), (40 * d).toInt())
            idleColor   = iconIdle
            activeColor = iconHover
            activeBgColor = iconHoverBg
            onSendClick = {
                val msg = editText.text.toString().trim()
                if (msg.isNotEmpty()) {
                    this@MessageBoxView.onSendClick?.invoke(msg)
                    editText.text.clear()
                }
            }
        }
        addView(sendBtn)
    }

    private fun animateBorder(focused: Boolean) {
        borderAnim.cancel()
        borderAnim.setIntValues(borderColor, if (focused) borderFocus else borderIdle)
        borderAnim.setEvaluator(ArgbEvaluator())
        borderAnim.start()
    }

    private fun invalidateSelf() { invalidate() }

    override fun onDraw(canvas: Canvas) {
        val r = 10 * d
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        // background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        canvas.drawRoundRect(rect, r, r, bgPaint)

        // border
        val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor; style = Paint.Style.STROKE; strokeWidth = 1 * d
        }
        canvas.drawRoundRect(rect, r, r, bp)
    }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = MeasureSpec.makeMeasureSpec((350 * d).toInt(), MeasureSpec.AT_MOST)
        val h = MeasureSpec.makeMeasureSpec((40 * d).toInt(), MeasureSpec.EXACTLY)
        super.onMeasure(w, h)
    }

    fun getText(): String = editText.text.toString()
    fun clearText() { editText.text.clear() }

    // ---- Inner views ----

    inner class AttachIconView(ctx: Context) : View(ctx) {
        var idleColor     = iconIdle
        var onHoverColor  = iconHover
        var onHoverBgColor = iconHoverBg
        var onAttachClick: (() -> Unit)? = null

        private var hoverFrac = 0f
        private val anim = ValueAnimator().apply {
            duration = 200
            addUpdateListener { hoverFrac = it.animatedValue as Float; invalidate() }
        }
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // tooltip
        private var showTooltip = false

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f; val cy = height / 2f
            val r  = 9 * d
            val col = blendColors(idleColor, onHoverColor, hoverFrac)

            // hover bg circle
            if (hoverFrac > 0f) {
                p.color = Color.argb((hoverFrac * 255).toInt(), 60, 60, 60)
                canvas.drawCircle(cx, cy, r + 4 * d, p)
            }

            // circle
            p.style = Paint.Style.STROKE; p.strokeWidth = 1.5f * d; p.color = col
            canvas.drawCircle(cx, cy, r, p)

            // + cross
            p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND; p.strokeWidth = 2f * d; p.color = col
            canvas.drawLine(cx, cy - 5 * d, cx, cy + 5 * d, p)
            canvas.drawLine(cx - 5 * d, cy, cx + 5 * d, cy, p)

            // tooltip
            if (showTooltip) drawTooltip(canvas, cx)
        }

        private fun drawTooltip(canvas: Canvas, cx: Float) {
            val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 9 * d; setTypeface(Typeface.create("sans-serif", Typeface.NORMAL)) }
            val text = "Add an image"
            val tw = tp.measureText(text)
            val th = tp.textSize
            val pad = 5 * d
            val tr = RectF(cx - tw / 2 - pad, -38 * d, cx + tw / 2 + pad, -20 * d)
            val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
            canvas.drawRoundRect(tr, 4 * d, 4 * d, bp)
            bp.color = Color.parseColor("#3c3c3c"); bp.style = Paint.Style.STROKE; bp.strokeWidth = 1 * d
            canvas.drawRoundRect(tr, 4 * d, 4 * d, bp)
            tp.color = Color.WHITE
            canvas.drawText(text, tr.left + pad, tr.bottom - pad, tp)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN  -> { animate(true);  showTooltip = true;  invalidate() }
                MotionEvent.ACTION_UP    -> { animate(false); showTooltip = false; onAttachClick?.invoke(); invalidate() }
                MotionEvent.ACTION_CANCEL -> { animate(false); showTooltip = false; invalidate() }
            }
            return true
        }

        private fun animate(on: Boolean) {
            anim.cancel(); anim.setFloatValues(hoverFrac, if (on) 1f else 0f); anim.start()
        }

        override fun onDetachedFromWindow() { super.onDetachedFromWindow(); anim.cancel() }
    }

    inner class SendIconView(ctx: Context) : View(ctx) {
        var idleColor     = iconIdle
        var activeColor   = iconHover
        var activeBgColor = iconHoverBg
        var hasText       = false
            set(v) { field = v; invalidate() }
        var onSendClick: (() -> Unit)? = null

        private var hoverFrac = 0f
        private val anim = ValueAnimator().apply {
            duration = 200
            addUpdateListener { hoverFrac = it.animatedValue as Float; invalidate() }
        }
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private val sendPath = Path()

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f; val cy = height / 2f
            val active = hasText || hoverFrac > 0f
            val col    = if (active) blendColors(idleColor, activeColor, maxOf(hoverFrac, if (hasText) 1f else 0f)) else idleColor

            // send arrow (paper plane style)
            val s = 10 * d
            sendPath.reset()
            sendPath.moveTo(cx + s, cy)           // tip right
            sendPath.lineTo(cx - s, cy - s * 0.8f) // top left
            sendPath.lineTo(cx - s * 0.2f, cy)    // center notch
            sendPath.lineTo(cx - s, cy + s * 0.8f) // bottom left
            sendPath.close()

            p.style = Paint.Style.FILL
            p.color = if (active) activeBgColor else Color.TRANSPARENT
            canvas.drawPath(sendPath, p)

            p.style = Paint.Style.STROKE
            p.strokeWidth = 2 * d
            p.strokeJoin  = Paint.Join.ROUND
            p.strokeCap   = Paint.Cap.ROUND
            p.color = col
            canvas.drawPath(sendPath, p)

            // center line
            p.style = Paint.Style.STROKE; p.strokeWidth = 2 * d; p.color = col
            canvas.drawLine(cx - s * 0.2f, cy, cx + s * 0.5f, cy, p)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN  -> animate(true)
                MotionEvent.ACTION_UP    -> { animate(false); onSendClick?.invoke() }
                MotionEvent.ACTION_CANCEL -> animate(false)
            }
            return true
        }

        private fun animate(on: Boolean) {
            anim.cancel(); anim.setFloatValues(hoverFrac, if (on) 1f else 0f); anim.start()
        }

        override fun onDetachedFromWindow() { super.onDetachedFromWindow(); anim.cancel() }
    }

    private fun blendColors(c1: Int, c2: Int, f: Float): Int {
        val t = f.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(c1)   + (Color.red(c2)   - Color.red(c1))   * t).toInt(),
            (Color.green(c1) + (Color.green(c2) - Color.green(c1)) * t).toInt(),
            (Color.blue(c1)  + (Color.blue(c2)  - Color.blue(c1))  * t).toInt()
        )
    }
}
