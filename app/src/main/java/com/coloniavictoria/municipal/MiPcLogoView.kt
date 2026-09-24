package com.coloniavictoria.municipal

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.min

/** Logo compacto de Mi PC, inspirado directamente en el logo oficial de la app. */
class MiPcLogoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lift = 0f
    private var glow = 0f
    private var animator: ValueAnimator? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2400L
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val p = it.animatedFraction
                lift = -3f * kotlin.math.sin(p * Math.PI * 2.0).toFloat()
                glow = (kotlin.math.sin(p * Math.PI * 2.0) + 1f) / 2f
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
        val size = min(width, height).toFloat()
        val scale = size / 120f
        val cx = width / 2f
        val cy = height / 2f + lift

        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(scale, scale)

        paint.style = Paint.Style.FILL
        paint.color = 0xFF15151B.toInt()
        canvas.drawRoundRect(RectF(-58f, -58f, 58f, 58f), 25f, 25f, paint)

        // Las cuatro bandas exteriores reproducen la composición del logo original.
        paint.color = 0xFFE53935.toInt()
        canvas.drawRoundRect(RectF(-48f, -48f, -4f, -4f), 9f, 9f, paint)
        paint.color = 0xFFFF8A3D.toInt()
        canvas.drawRoundRect(RectF(4f, -48f, 48f, -4f), 9f, 9f, paint)
        paint.color = 0xFF36B36B.toInt()
        canvas.drawRoundRect(RectF(-48f, 4f, -4f, 48f), 9f, 9f, paint)
        paint.color = 0xFF3197D6.toInt()
        canvas.drawRoundRect(RectF(4f, 4f, 48f, 48f), 9f, 9f, paint)

        // Centro blanco y sus cuatro módulos de color.
        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawRoundRect(RectF(-30f, -30f, 30f, 30f), 10f, 10f, paint)
        paint.color = 0xFFE53935.toInt()
        canvas.drawRoundRect(RectF(-20f, -20f, -2f, -2f), 4f, 4f, paint)
        paint.color = 0xFFFF8A3D.toInt()
        canvas.drawRoundRect(RectF(2f, -20f, 20f, -2f), 4f, 4f, paint)
        paint.color = 0xFF36B36B.toInt()
        canvas.drawRoundRect(RectF(-20f, 2f, -2f, 20f), 4f, 4f, paint)
        paint.color = 0xFF3197D6.toInt()
        canvas.drawRoundRect(RectF(2f, 2f, 20f, 20f), 4f, 4f, paint)

        // Halo muy sutil para que el logo tenga vida sin parecer un efecto infantil.
        paint.color = 0x229A7BFF.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f + glow * 1.5f
        canvas.drawRoundRect(RectF(-61f, -61f, 61f, 61f), 28f, 28f, paint)

        canvas.restore()
    }
}
