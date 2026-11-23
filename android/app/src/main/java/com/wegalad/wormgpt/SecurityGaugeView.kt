package com.wegalad.wormgpt

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat

/**
 * Animated protection-score gauge: a 270° arc with tick marks, a breathing glow,
 * and a slow scanner sweep. The arc length and colour follow the current score.
 */
class SecurityGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sweepPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arcRect = RectF()

    private var glowGradient: RadialGradient? = null
    private var glowRadius = 0f
    private var pulse = 0f
    private var rotation = 0f
    private var displayScore = 0f
    private var targetScore = 0f

    private var pulseAnimator: ValueAnimator? = null
    private var rotationAnimator: ValueAnimator? = null
    private var scoreAnimator: ValueAnimator? = null

    private val startAngle = 135f
    private val sweepAngle = 270f

    init {
        setWillNotDraw(false)
    }

    private fun animationsEnabled(): Boolean =
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f

    fun setScore(score: Int, animate: Boolean = true) {
        val clamped = score.coerceIn(0, 100).toFloat()
        targetScore = clamped
        glowGradient = null
        scoreAnimator?.cancel()
        if (!animate || !animationsEnabled()) {
            displayScore = clamped
            invalidate()
            return
        }
        scoreAnimator = ValueAnimator.ofFloat(displayScore, clamped).apply {
            duration = 900L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                displayScore = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAmbientAnimations()
    }

    override fun onDetachedFromWindow() {
        stopAmbientAnimations()
        scoreAnimator?.cancel()
        scoreAnimator = null
        super.onDetachedFromWindow()
    }

    private fun startAmbientAnimations() {
        if (!animationsEnabled() || pulseAnimator != null) return
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2200L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                pulse = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
        rotationAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 7000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                rotation = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopAmbientAnimations() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        rotationAnimator?.cancel()
        rotationAnimator = null
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val cx = w / 2f
        val cy = h / 2f
        val size = minOf(w, h)
        val radius = size * 0.42f
        val stroke = size * 0.075f

        // Breathing glow behind the gauge.
        val glowR = size * 0.62f
        if (glowGradient == null || glowRadius != glowR) {
            glowGradient = RadialGradient(
                cx,
                cy,
                glowR,
                scoreColor(displayScore),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            glowRadius = glowR
        }
        glowPaint.shader = glowGradient
        glowPaint.alpha = ((0.08f + 0.12f * pulse) * 255f).toInt().coerceIn(0, 255)
        canvas.drawCircle(cx, cy, glowR, glowPaint)

        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)

        // Track.
        trackPaint.style = Paint.Style.STROKE
        trackPaint.strokeCap = Paint.Cap.ROUND
        trackPaint.strokeWidth = stroke
        trackPaint.color = withAlpha(ContextCompat.getColor(context, R.color.sentinal_border), 0.55f)
        canvas.drawArc(arcRect, startAngle, sweepAngle, false, trackPaint)

        // Tick marks every 10%.
        tickPaint.style = Paint.Style.STROKE
        tickPaint.strokeCap = Paint.Cap.ROUND
        tickPaint.strokeWidth = size * 0.015f
        for (index in 0..10) {
            val angle = Math.toRadians((startAngle + sweepAngle * index / 10f).toDouble())
            val inner = radius - stroke * 0.9f
            val outer = radius + stroke * 0.9f
            val x1 = cx + (inner * Math.cos(angle)).toFloat()
            val y1 = cy + (inner * Math.sin(angle)).toFloat()
            val x2 = cx + (outer * Math.cos(angle)).toFloat()
            val y2 = cy + (outer * Math.sin(angle)).toFloat()
            val reached = displayScore >= index * 10f
            tickPaint.color = withAlpha(
                if (reached) scoreColor(displayScore) else ContextCompat.getColor(context, R.color.sentinal_faint),
                if (reached) 0.9f else 0.35f,
            )
            canvas.drawLine(x1, y1, x2, y2, tickPaint)
        }

        // Progress arc.
        val fraction = (displayScore / 100f).coerceIn(0f, 1f)
        progressPaint.style = Paint.Style.STROKE
        progressPaint.strokeCap = Paint.Cap.ROUND
        progressPaint.strokeWidth = stroke
        progressPaint.color = scoreColor(displayScore)
        if (fraction > 0f) {
            canvas.drawArc(arcRect, startAngle, sweepAngle * fraction, false, progressPaint)
        }

        // Slow scanner sweep riding the track.
        if (animationsEnabled()) {
            sweepPaint.style = Paint.Style.STROKE
            sweepPaint.strokeCap = Paint.Cap.ROUND
            sweepPaint.strokeWidth = stroke * 0.55f
            sweepPaint.color = withAlpha(Color.WHITE, 0.18f + 0.16f * pulse)
            val position = rotation % sweepAngle
            val sweepLength = minOf(26f, sweepAngle - position)
            if (sweepLength > 0f) {
                canvas.drawArc(arcRect, startAngle + position, sweepLength, false, sweepPaint)
            }
        }
    }

    private fun scoreColor(score: Float): Int {
        val red = ContextCompat.getColor(context, R.color.sentinal_red_bright)
        val amber = ContextCompat.getColor(context, R.color.sentinal_amber)
        val green = ContextCompat.getColor(context, R.color.sentinal_emerald)
        val evaluator = ArgbEvaluator()
        return if (score <= 50f) {
            evaluator.evaluate(score / 50f, red, amber) as Int
        } else {
            evaluator.evaluate((score - 50f) / 50f, amber, green) as Int
        }
    }

    private fun withAlpha(color: Int, alpha: Float): Int {
        val safe = (alpha * 255f).toInt().coerceIn(0, 255)
        return (safe shl 24) or (color and 0x00FFFFFF)
    }
}
