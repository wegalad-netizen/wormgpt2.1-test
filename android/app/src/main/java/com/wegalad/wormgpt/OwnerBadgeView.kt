package com.wegalad.wormgpt

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.provider.Settings
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat

/**
 * The Sentinal owner seal: the shield mark inside a breathing glow with two
 * counter-rotating scanner arcs. Used on the owner cards, the launch screen and
 * the owner profile dialog.
 */
class OwnerBadgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sweepPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outerRect = RectF()
    private val innerRect = RectF()
    private val iconDst = RectF()

    private val shield: Bitmap? = renderShield(context)
    private var glowGradient: RadialGradient? = null
    private var glowRadius = 0f
    private var pulse = 0f
    private var rotation = 0f
    private var pulseAnimator: ValueAnimator? = null
    private var rotationAnimator: ValueAnimator? = null

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
        startAnimations()
    }

    override fun onDetachedFromWindow() {
        stopAnimations()
        super.onDetachedFromWindow()
    }

    private fun animationsEnabled(): Boolean =
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f

    private fun startAnimations() {
        if (!animationsEnabled() || pulseAnimator != null) return

        pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
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

        rotationAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 6200
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { valueAnimator ->
                rotation = valueAnimator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopAnimations() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        rotationAnimator?.cancel()
        rotationAnimator = null
    }

    override fun onDraw(canvas: Canvas) {
        val icon = shield ?: return
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val cx = w / 2f
        val cy = h / 2f
        val size = minOf(w, h)

        // Breathing radial glow behind everything.
        val radius = size * 0.68f
        if (glowGradient == null || glowRadius != radius) {
            glowGradient = RadialGradient(
                cx,
                cy,
                radius,
                ContextCompat.getColor(context, R.color.sentinal_cyan),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            glowRadius = radius
        }
        glowPaint.shader = glowGradient
        glowPaint.alpha = ((0.10f + 0.14f * pulse) * 255f).toInt().coerceIn(0, 255)
        canvas.drawCircle(cx, cy, radius, glowPaint)

        // Outer scanner track and rotating red sweep.
        val outerRadius = size * 0.52f
        outerRect.set(cx - outerRadius, cy - outerRadius, cx + outerRadius, cy + outerRadius)
        ringPaint.style = Paint.Style.STROKE
        ringPaint.strokeCap = Paint.Cap.ROUND
        ringPaint.strokeWidth = size * 0.045f
        ringPaint.color = ringColor(0.16f + 0.14f * pulse)
        canvas.drawCircle(cx, cy, outerRadius, ringPaint)

        sweepPaint.style = Paint.Style.STROKE
        sweepPaint.strokeCap = Paint.Cap.ROUND
        sweepPaint.strokeWidth = size * 0.075f
        sweepPaint.color = ringColor(0.72f + 0.26f * pulse)
        canvas.drawArc(outerRect, rotation, 62f, false, sweepPaint)

        // Inner counter-rotating cyan sweep.
        val innerRadius = size * 0.38f
        innerRect.set(cx - innerRadius, cy - innerRadius, cx + innerRadius, cy + innerRadius)
        ringPaint.strokeWidth = size * 0.03f
        ringPaint.color = cyanColor(0.18f + 0.14f * pulse)
        canvas.drawCircle(cx, cy, innerRadius, ringPaint)

        sweepPaint.strokeWidth = size * 0.05f
        sweepPaint.color = cyanColor(0.60f + 0.30f * pulse)
        canvas.drawArc(innerRect, -rotation * 1.45f, 48f, false, sweepPaint)

        // Shield mark in the center.
        val iconSize = size * 0.56f
        iconDst.set(
            cx - iconSize / 2f,
            cy - iconSize / 2f,
            cx + iconSize / 2f,
            cy + iconSize / 2f,
        )
        canvas.drawBitmap(icon, null, iconDst, iconPaint)
    }

    private fun ringColor(alpha: Float): Int {
        val base = ContextCompat.getColor(context, R.color.sentinal_red_bright)
        val safeAlpha = (alpha * 255f).toInt().coerceIn(0, 255)
        return (safeAlpha shl 24) or (base and 0x00FFFFFF)
    }

    private fun cyanColor(alpha: Float): Int {
        val base = ContextCompat.getColor(context, R.color.sentinal_cyan)
        val safeAlpha = (alpha * 255f).toInt().coerceIn(0, 255)
        return (safeAlpha shl 24) or (base and 0x00FFFFFF)
    }
}
