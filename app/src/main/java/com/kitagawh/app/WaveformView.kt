package com.kitagawh.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

/**
 * Animated row of dashes that shake and dance while Jarvis is listening or
 * speaking - the live voice waveform in the Jarvis overlay panel.
 */
class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var phase = 0f
    var active = true

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bars = 21
        val spacing = width / bars.toFloat()
        paint.strokeWidth = spacing * 0.42f
        val midY = height / 2f
        for (i in 0 until bars) {
            val wave = if (active) {
                (sin(phase + i * 0.55) * 0.5 + 0.5).toFloat()
            } else {
                0.12f
            }
            val half = 4f + wave * (height / 2f - 6f)
            val x = spacing * i + spacing / 2f
            paint.alpha = 140 + (wave * 115).toInt()
            canvas.drawLine(x, midY - half, x, midY + half, paint)
        }
        if (active) {
            phase += 0.28f
            postInvalidateDelayed(40)
        }
    }

    fun startAnimating() {
        active = true
        invalidate()
    }

    fun stopAnimating() {
        active = false
        invalidate()
    }
}
