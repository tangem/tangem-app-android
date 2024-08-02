package com.tangem.core.ui.utils

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

const val GRAY_SCALE_SATURATION = 0f
const val GRAY_SCALE_ALPHA = 0.4f
const val NORMAL_ALPHA = 1f

val GrayscaleColorFilter: ColorFilter
    get() = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(GRAY_SCALE_SATURATION) })

/**
 * Returns alpha and color filter for grayscale if [isGrayscale] is true
 */
fun getGreyScaleColorFilter(isGrayscale: Boolean): Pair<Float, ColorFilter?> = if (isGrayscale) {
    GRAY_SCALE_ALPHA to GrayscaleColorFilter
} else {
    NORMAL_ALPHA to null
}