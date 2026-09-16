package com.ivan.asistente

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.min

class IntroRobotView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lift = 0f
    private var animator: ValueAnimator? = null
    private var observer: RecyclerView.AdapterDataObserver? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        animator = ValueAnimator.ofFloat(0f, -5f, 0f).apply {
            duration = 2200L
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                lift = it.animatedValue as Float
                invalidate()
            }
            start()
        }

        post {
            val recycler = rootView.findViewById<RecyclerView>(R.id.messages) ?: return@post
            val panel = parent as? View ?: return@post

            fun syncVisibility() {
                panel.visibility = if (recycler.adapter?.itemCount ?: 0 == 0) View.VISIBLE else View.GONE
            }

            observer = object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() = syncVisibility()
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = syncVisibility()
                override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = syncVisibility()
                override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) = syncVisibility()
                override fun onItemRangeChanged(positionStart: Int, itemCount: Int) = syncVisibility()
            }
            recycler.adapter?.registerAdapterDataObserver(observer!!)
            syncVisibility()
        }
    }

    override fun onDetachedFromWindow() {
        val recycler = rootView.findViewById<RecyclerView>(R.id.messages)
        observer?.let { recycler?.adapter?.unregisterAdapterDataObserver(it) }
        observer = null
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val s = min(width, height).toFloat()
        val cx = width / 2f
        val cy = height / 2f + lift
        val scale = s / 92f

        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(scale, scale)

        paint.style = Paint.Style.FILL
        paint.color = 0xFF8B6CFF.toInt()
        canvas.drawRoundRect(RectF(-31f, -26f, 31f, 25f), 14f, 14f, paint)

        paint.color = 0xFF202027.toInt()
        canvas.drawRoundRect(RectF(-26f, -21f, 26f, 20f), 10f, 10f, paint)

        paint.color = 0xFFB9A7FF.toInt()
        canvas.drawCircle(-10f, -1f, 4f, paint)
        canvas.drawCircle(10f, -1f, 4f, paint)

        paint.color = 0xFF8B6CFF.toInt()
        canvas.drawRoundRect(RectF(-22f, 25f, 22f, 31f), 3f, 3f, paint)

        paint.strokeWidth = 4f
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        paint.color = 0xFF8B6CFF.toInt()
        canvas.drawLine(0f, -31f, 0f, -38f, paint)

        paint.style = Paint.Style.FILL
        paint.color = 0xFFB9A7FF.toInt()
        canvas.drawCircle(0f, -41f, 4f, paint)

        paint.strokeWidth = 3.5f
        paint.style = Paint.Style.STROKE
        paint.color = 0xFF8B6CFF.toInt()
        canvas.drawLine(-31f, -4f, -39f, 3f, paint)
        canvas.drawLine(31f, -4f, 39f, 3f, paint)

        canvas.restore()
    }
}
