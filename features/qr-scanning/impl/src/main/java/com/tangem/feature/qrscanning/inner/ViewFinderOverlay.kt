package com.tangem.feature.qrscanning.inner

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.tangem.feature.qrscanning.impl.R

class ViewFinderOverlay(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val boxPaint: Paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.white)
        style = Paint.Style.STROKE
        strokeWidth = context.resources.getDimensionPixelOffset(R.dimen.qr_border_stroke_width).toFloat()
    }

    private val boxWidthRatio = 0.8F
    private val boxCornerRadius: Float =
        context.resources.getDimensionPixelOffset(R.dimen.qr_border_corner_radius).toFloat()

    private var boxRect: RectF? = null

    @Suppress("MagicNumber")
    fun setViewFinder() {
        val overlayWidth = width.toFloat()
        val overlayHeight = height.toFloat()
        val boxSize = overlayWidth * boxWidthRatio
        val cx = overlayWidth / 2
        val cy = overlayHeight / 2
        boxRect = RectF(cx - boxSize / 2, cy - boxSize / 2, cx + boxSize / 2, cy + boxSize / 2)

        invalidate()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        boxRect?.let {
            canvas.drawRoundRect(it, boxCornerRadius, boxCornerRadius, boxPaint)
        }
    }
}