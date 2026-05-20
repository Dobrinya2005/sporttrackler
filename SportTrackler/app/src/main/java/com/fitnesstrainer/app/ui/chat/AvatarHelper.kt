package com.fitnesstrainer.app.ui.chat

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable

class InitialsDrawable(private val initials: String, private val color: Int) : Drawable() {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val cx = b.exactCenterX()
        val cy = b.exactCenterY()
        val radius = minOf(b.width(), b.height()) / 2f
        canvas.drawCircle(cx, cy, radius, bgPaint)
        textPaint.textSize = radius * 0.85f
        val textY = cy - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(initials, cx, textY, textPaint)
    }

    override fun setAlpha(alpha: Int) { bgPaint.alpha = alpha }
    override fun setColorFilter(cf: ColorFilter?) { bgPaint.colorFilter = cf }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

object AvatarHelper {

    private val PALETTE = listOf(
        0xFF5B6EF5.toInt(), // indigo
        0xFF26C6DA.toInt(), // cyan
        0xFF66BB6A.toInt(), // green
        0xFFEF5350.toInt(), // red
        0xFFFF7043.toInt(), // deep orange
        0xFFAB47BC.toInt(), // purple
        0xFF42A5F5.toInt(), // blue
        0xFFEC407A.toInt(), // pink
        0xFF26A69A.toInt(), // teal
        0xFFFFCA28.toInt(), // amber
    )

    fun forName(name: String): InitialsDrawable {
        val trimmed = name.trim()
        val parts = trimmed.split(" ").filter { it.isNotBlank() }
        val initials = when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "?"
        }
        val color = PALETTE[Math.abs(trimmed.hashCode()) % PALETTE.size]
        return InitialsDrawable(initials, color)
    }
}
