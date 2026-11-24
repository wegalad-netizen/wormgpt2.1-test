package com.wegalad.wormgpt

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.random.Random

/** Slow drifting ambient dots behind chat messages. */
class ChatParticlesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private data class Dot(
        val x: Float,
        val y: Float,
        val radius: Float,
        val speed: Float,
        val phase: Float,
        val color: Int,
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dots = mutableListOf<Dot>()
    private var progress = 0f
    private var animator: ValueAnimator? = null

    private val colors = intArrayOf(
        Color.rgb(6, 182, 212),
        Color.rgb(16, 185, 129),
        Color.rgb(239, 68, 68),
    )

    init {
        setWillNotDraw(false)
        repeat(22) { index ->
            dots += Dot(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = 1.2f + Random.nextFloat() * 1.8f,
                speed = 0.25f + Random.nextFloat() * 0.6f,
                phase = Random.nextFloat() * 6.28f,
                color = colors[index % colors.size],
            )
        }
    }

    private fun animationsEnabled(): Boolean =
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animationsEnabled() || animator != null) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 14_000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                progress = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        dots.forEach { dot ->
            val t = if (animationsEnabled()) progress else 0f
            val y = ((dot.y - t * dot.speed) % 1f + 1f) % 1f * h
            val sway = (Math.sin((t * 6.28f + dot.phase).toDouble()) * 10.0).toFloat()
            val x = dot.x * w + sway
            val alpha =
                0.10f + 0.16f * Math.abs(Math.sin((t * 3.14f + dot.phase).toDouble())).toFloat()
            paint.color = dot.color
            paint.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
            canvas.drawCircle(x, y, dot.radius, paint)
        }
    }
}
