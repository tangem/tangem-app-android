package com.tangem.core.ui.utils

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageBackgroundContrastChecker(
    private val image: Bitmap,
    private val backgroundColor: Int,
) {
    constructor(drawable: Drawable, backgroundColor: Int, size: Int) : this(
        image = drawable.toBitmap(width = size, height = size),
        backgroundColor = backgroundColor,
    )

    suspend fun getContrastColor(isDarkTheme: Boolean): Color {
        return if (isLowContrast()) {
            if (isDarkTheme) Color.White else Color.Black
        } else {
            Color.Transparent
        }
    }

    private suspend fun isLowContrast(): Boolean {
        val palette = generatePaletteAsync(bitmap = image)
        val color = palette.getDominantColor(backgroundColor)
        val contrast = ColorUtils.calculateContrast(color, backgroundColor)
        return contrast <= LOW_CONTRAST_RATIO
    }

    private suspend fun generatePaletteAsync(bitmap: Bitmap): Palette = withContext(Dispatchers.Default) {
        return@withContext Palette.Builder(bitmap).generate()
    }

    private companion object {
        // https://www.w3.org/TR/2008/REC-WCAG20-20081211/#contrast-ratiodef
        private const val LOW_CONTRAST_RATIO = 1f
    }
}