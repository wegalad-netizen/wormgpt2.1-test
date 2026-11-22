package com.wegalad.wormgpt

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat

/**
 * The Sentinal owner seal: the shield mark with a breathing ring and a soft
 * pulsing glow. Used on the owner cards and in the owner profile dialog.
 */
class OwnerBadgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconDst = RectF()

    private var shield: Bitmap? = renderShield(context)
    private var pulse = 0f
    private var animator: ValueAnimator? = null

    init {
        setWillNotDraw(false)
    }

    private fun renderShield(context: Context): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, R.drawable.security_shield_icon)
            ?: return null
        val size = dp(context, 96)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2400
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { valueAnimator ->
                pulse = valueAnimator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val icon = shield ?: return
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val cx = w / 2f
        val cy = h / 2f
        val size = minOf(w, h)

        // Soft glow behind everything.
        glowPaint.color = glowColor()
        canvas.drawCircle(cx, cy, size * 0.66f, glowPaint)

        // Pulsing outer ring.
        ringPaint.style = Paint.Style.STROKE
        ringPaint.strokeCap = Paint.Cap.ROUND
        ringPaint.strokeWidth = size * 0.075f
        ringPaint.color = ringColor()
        canvas.drawCircle(cx, cy, size * 0.52f, ringPaint)

        // Inner ring, phase-shifted against the outer one.
        ringPaint.strokeWidth = size * 0.045f
        ringPaint.color = ringColor()
        canvas.drawCircle(cx, cy, size * 0.39f, ringPaint)

        // Shield mark in the center.
        val iconSize = size * 0.58f
        iconDst.set(
            cx - iconSize / 2f,
            cy - iconSize / 2f,
            cx + iconSize / 2f,
            cy + iconSize / 2f,
        )
        canvas.drawBitmap(icon, null, iconDst, iconPaint)
    }

    private fun glowColor(): Int {
        val base = ContextCompat.getColor(context, R.color.sentinal_cyan)
        val alpha = (0.08f + 0.13f * pulse).coerceIn(0.08f, 0.21f)
        return ((alpha * 255f).toInt() shl 24) or (base and 0x00FFFFFF)
    }

    private fun ringColor(): Int {
        val base = ContextCompat.getColor(context, R.color.sentinal_red_bright)
        val alpha = (0.40f + 0.55f * pulse).coerceIn(0.40f, 0.95f)
        return ((alpha * 255f).toInt() shl 24) or (base and 0x00FFFFFF)
    }
}
