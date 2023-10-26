package com.tangem.tap.features.details.ui.walletconnect

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.tangem.wallet.R

class ViewFinderOverlay(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val boxPaint: Paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.white)
        style = Paint.Style.STROKE
        strokeWidth = context.resources.getDimensionPixelOffset(R.dimen.qr_border_stroke_width).toFloat()
    }

    private val boxCornerRadius: Float =
        context.resources.getDimensionPixelOffset(R.dimen.qr_border_corner_radius).toFloat()

    private var boxRect: RectF? = null

    fun setViewFinder() {
        val overlayWidth = width.toFloat()
        val overlayHeight = height.toFloat()
        val boxWidth = overlayWidth * 80 / 100
        val boxHeight = overlayHeight * 36 / 100
        val cx = overlayWidth / 2
        val cy = overlayHeight / 2
        boxRect = RectF(cx - boxWidth / 2, cy - boxHeight / 2, cx + boxWidth / 2, cy + boxHeight / 2)

        invalidate()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        boxRect?.let {
            canvas.drawRoundRect(it, boxCornerRadius, boxCornerRadius, boxPaint)
        }
    }

}