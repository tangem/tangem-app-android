package com.tangem.features.details.ui.coil

import android.graphics.Bitmap
import android.graphics.Matrix
import coil.size.Size
import coil.transform.Transformation

internal class RotationTransformation(private val angle: Float) : Transformation {

    override val cacheKey: String = "rotate:$angle"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val matrix = Matrix().apply {
            val centerX = input.width / 2f
            val centerY = input.height / 2f

            postRotate(angle, centerX, centerY)
        }

        return Bitmap.createBitmap(input, 0, 0, input.width, input.height, matrix, true)
    }
}
