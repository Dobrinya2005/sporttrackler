package com.fitnesstrainer.app.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.fitnesstrainer.app.R
import kotlin.math.min

class GlassBottomNav @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class NavItem(val label: String, val iconRes: Int)

    private val d = resources.displayMetrics.density

    private var items = emptyList<NavItem>()
    private var badges = IntArray(0)

    private var selectedIndex = 0
    private var pressAlphas   = FloatArray(items.size)
    private var pressAnimators = arrayOfNulls<ValueAnimator>(items.size)

    private val bgBlue       = Color.argb(255, 50, 60, 160)
    private val activeTab    = Color.argb(255, 255, 255, 255)
    private val activeText   = Color.argb(255, 30,  40,  120)
    private val inactiveText = Color.argb(255, 200, 210, 255)

    var onItemSelected: ((Int) -> Unit)? = null

    private val bgPaint      = Paint(Paint.ANTI_ALIAS_FLAG)
    private val itemBgPaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint    = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val textPaint    = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize  = 10 * d
        typeface  = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 1.5f * d
    }
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EF4444")
    }
    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }

    private val iconBitmaps = mutableListOf<Bitmap>()
    private var iconSize = 0f
    private var itemW    = 0f
    private var itemH    = 0f

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        itemW    = w / items.size.toFloat()
        itemH    = h.toFloat()
        iconSize = min(itemW, itemH) * 0.38f
        rebuildBitmaps()
    }

    private fun rebuildBitmaps() {
        iconBitmaps.forEach { it.recycle() }
        iconBitmaps.clear()
        val sz = iconSize.toInt().coerceAtLeast(1)
        items.forEach { item ->
            val drawable: Drawable = ContextCompat.getDrawable(context, item.iconRes)!!.mutate()
            val bmp = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
            val c   = Canvas(bmp)
            drawable.setBounds(0, 0, sz, sz)
            drawable.draw(c)
            iconBitmaps.add(bmp)
        }
    }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = MeasureSpec.getSize(wSpec).takeIf { it > 0 } ?: (360 * d).toInt()
        val h = (68 * d).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        val W = width.toFloat(); val H = height.toFloat()
        val r = H / 2f

        bgPaint.color = bgBlue
        canvas.drawRoundRect(RectF(0f, 0f, W, H), r, r, bgPaint)

        // no glass highlight

        items.indices.forEach { i -> drawItem(canvas, i) }
    }

    private fun drawItem(canvas: Canvas, i: Int) {
        val cx      = itemW * i + itemW / 2f
        val pad     = 6 * d
        val rect    = RectF(itemW * i + pad, pad, itemW * (i + 1) - pad, height - pad)
        val ir      = rect.height() / 2f
        val isActive = i == selectedIndex
        val pressF   = pressAlphas[i]

        when {
            isActive -> { itemBgPaint.color = activeTab; canvas.drawRoundRect(rect, ir, ir, itemBgPaint) }
            pressF > 0f -> { itemBgPaint.color = Color.argb((pressF * 77).toInt(), 255, 255, 255); canvas.drawRoundRect(rect, ir, ir, itemBgPaint) }
        }

        val iconColor = if (isActive) activeText else inactiveText
        val bmp = iconBitmaps.getOrNull(i)
        if (bmp != null && !bmp.isRecycled) {
            iconPaint.colorFilter = PorterDuffColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
            val iconX = cx - iconSize / 2f
            val iconY = rect.top + rect.height() * 0.12f
            canvas.drawBitmap(bmp, iconX, iconY, iconPaint)
            iconPaint.colorFilter = null
        }

        textPaint.color = if (isActive) activeText else inactiveText
        canvas.drawText(items[i].label, cx, rect.bottom - 7 * d, textPaint)

        // Badge
        val count = badges.getOrElse(i) { 0 }
        if (count > 0) {
            val bmp = iconBitmaps.getOrNull(i)
            val iconX = cx - iconSize / 2f
            val iconY = rect.top + rect.height() * 0.12f
            val badgeR = 8f * d
            val badgeCx = iconX + iconSize - badgeR * 0.3f
            val badgeCy = iconY + badgeR * 0.3f
            canvas.drawCircle(badgeCx, badgeCy, badgeR, badgePaint)
            badgeTextPaint.textSize = 8f * d
            val text = if (count > 99) "99+" else count.toString()
            val fm = badgeTextPaint.fontMetrics
            canvas.drawText(text, badgeCx, badgeCy - (fm.ascent + fm.descent) / 2f, badgeTextPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val idx = (event.x / itemW).toInt().coerceIn(0, items.size - 1)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> animatePress(idx, true)
            MotionEvent.ACTION_UP   -> {
                animatePress(idx, false)
                if (idx != selectedIndex) {
                    selectedIndex = idx
                    onItemSelected?.invoke(idx)
                    invalidate()
                }
            }
            MotionEvent.ACTION_CANCEL -> animatePress(idx, false)
        }
        return true
    }

    private fun animatePress(idx: Int, down: Boolean) {
        pressAnimators[idx]?.cancel()
        pressAnimators[idx] = ValueAnimator.ofFloat(pressAlphas[idx], if (down) 1f else 0f).apply {
            duration = 180
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { pressAlphas[idx] = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    fun setSelected(index: Int) {
        if (items.isEmpty()) return
        selectedIndex = index.coerceIn(0, items.size - 1)
        invalidate()
    }

    fun setItems(newItems: List<NavItem>) {
        pressAnimators.forEach { it?.cancel() }
        iconBitmaps.forEach { it.recycle() }
        iconBitmaps.clear()
        items = newItems
        selectedIndex = 0
        pressAlphas = FloatArray(newItems.size)
        pressAnimators = arrayOfNulls(newItems.size)
        badges = IntArray(newItems.size)
        if (width > 0) {
            itemW = width / newItems.size.toFloat()
            rebuildBitmaps()
        }
        invalidate()
    }

    fun setBadge(tabIndex: Int, count: Int) {
        if (tabIndex < 0 || tabIndex >= items.size) return
        if (badges.getOrElse(tabIndex) { -1 } == count) return
        if (badges.size != items.size) badges = IntArray(items.size)
        badges[tabIndex] = count
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pressAnimators.forEach { it?.cancel() }
        iconBitmaps.forEach { it.recycle() }
        iconBitmaps.clear()
    }
}
