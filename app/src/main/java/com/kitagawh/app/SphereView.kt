package com.kitagawh.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The Kitagawh sphere: a glowing orb with rotating energy arcs and orbiting
 * particles, drawn on Canvas and animated continuously. It never stops
 * rotating while it is on screen, which gives the home page its live,
 * AI-aesthetic feel.
 */
class SphereView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var angle = 0f
    private var sphereGradient: RadialGradient? = null
    private var cx = 0f
    private var cy = 0f
    private var radius = 0f

    private val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val spherePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    init {
        postInvalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cx = w / 2f
        cy = h / 2f
        radius = min(w, h) * 0.30f
        sphereGradient = RadialGradient(
            cx - radius * 0.35f, cy - radius * 0.35f, radius * 1.5f,
            intArrayOf(
                Color.parseColor("#B388FF"),
                Color.parseColor("#7C4DFF"),
                Color.parseColor("#311B92")
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        spherePaint.shader = sphereGradient
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Soft aura glow behind the sphere.
        for (i in 5 downTo 1) {
            auraPaint.color = Color.argb(14 * i, 124, 77, 255)
            canvas.drawCircle(cx, cy, radius + i * 14f, auraPaint)
        }
        auraPaint.color = Color.argb(30, 0, 229, 255)
        canvas.drawCircle(cx, cy, radius + 26f, auraPaint)

        // Sphere body.
        canvas.drawCircle(cx, cy, radius, spherePaint)

        // Rotating arcs at different depths and speeds.
        drawArc(canvas, radius * 0.98f, angle, 110f, 7f, Color.argb(220, 0, 229, 255))
        drawArc(canvas, radius * 0.82f, -angle * 1.4f + 60f, 90f, 6f, Color.argb(200, 255, 255, 255))
        drawArc(canvas, radius * 1.14f, angle * 0.7f + 180f, 70f, 5f, Color.argb(170, 179, 136, 255))
        drawArc(canvas, radius * 0.62f, angle * 2f + 120f, 130f, 5f, Color.argb(190, 0, 229, 255))

        // Orbiting particles.
        dotPaint.color = Color.parseColor("#00E5FF")
        for (i in 0 until 3) {
            val orbit = radius * (1.25f + i * 0.13f)
            val a = Math.toRadians((angle * (0.9 + i * 0.35) + i * 120).toDouble())
            val x = cx + (cos(a) * orbit).toFloat()
            val y = cy + (sin(a) * orbit * 0.96f).toFloat()
            canvas.drawCircle(x, y, 5f - i, dotPaint)
        }

        // Calm "-_-" core: two eye lines and a flat mouth, drawn as shapes.
        corePaint.alpha = 235
        val eyeY = cy - radius * 0.08f
        val eyeLen = radius * 0.20f
        val eyeGap = radius * 0.26f
        canvas.drawRect(cx - eyeGap - eyeLen, eyeY - 3f, cx - eyeGap, eyeY + 3f, corePaint)
        canvas.drawRect(cx + eyeGap, eyeY - 3f, cx + eyeGap + eyeLen, eyeY + 3f, corePaint)
        val mouthY = cy + radius * 0.22f
        canvas.drawRect(cx - eyeLen * 0.8f, mouthY - 3f, cx + eyeLen * 0.8f, mouthY + 3f, corePaint)

        angle = (angle + 1.6f) % 360f
        postInvalidateDelayed(16)
    }

    private fun drawArc(
        canvas: Canvas, r: Float, start: Float, sweep: Float,
        width: Float, color: Int
    ) {
        arcPaint.strokeWidth = width
        arcPaint.color = color
        val rect = RectF(cx - r, cy - r, cx + r, cy + r)
        canvas.drawArc(rect, start, sweep, false, arcPaint)
    }
}
