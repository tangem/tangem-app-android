package com.tangem.common.ui.charts.layer

import android.graphics.LinearGradient
import android.graphics.RectF
import android.graphics.Shader
import com.patrykandpatrick.vico.core.common.DrawContext
import com.patrykandpatrick.vico.core.common.Point
import com.patrykandpatrick.vico.core.common.shader.CacheableDynamicShader

/**
 * Horizontal gradient with a hard split at [splitFraction]: [leftColor] before it, [rightColor] after it.
 *
 * Equivalent to `DynamicShader.horizontalGradient(intArrayOf(leftColor, rightColor), floatArrayOf(f, f))`,
 * except that [getColorAt] is computed analytically. Vico's `BaseDynamicShader.getColorAt` rasterizes the shader
 * into a bitmap cached under a bounds-independent key, so once the chart is resized it reads a pixel outside
 * the stale bitmap and throws `IllegalArgumentException: x must be < bitmap.width()`.
 *
 * Must stay a data class: `rememberSplitLine` keys its `remember` on the shader, so identity equality
 * would recreate the line on every recomposition.
 */
internal data class SplitLineShader(
    val leftColor: Int,
    val rightColor: Int,
    val splitFraction: Float,
) : CacheableDynamicShader() {

    override fun createShader(context: DrawContext, left: Float, top: Float, right: Float, bottom: Float): Shader {
        return LinearGradient(
            left,
            top,
            right,
            top,
            intArrayOf(leftColor, rightColor),
            floatArrayOf(splitFraction, splitFraction),
            Shader.TileMode.CLAMP,
        )
    }

    override fun getColorAt(point: Point, context: DrawContext, bounds: RectF): Int {
        val width = bounds.width()
        val fraction = if (width > 0f) (point.x - bounds.left) / width else 0f

        return if (fraction < splitFraction) leftColor else rightColor
    }
}