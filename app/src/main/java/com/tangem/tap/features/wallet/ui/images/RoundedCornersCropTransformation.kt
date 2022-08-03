package com.tangem.tap.features.wallet.ui.images

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.annotation.Px
import androidx.core.graphics.applyCanvas
import coil.size.Size
import coil.size.pxOrElse
import coil.transform.Transformation

class RoundedCornersCropTransformation(
    @Px val radiusPx: Float,
) : Transformation {
    override val cacheKey: String = "${javaClass.name}-$radiusPx"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val outputWidth = size.width.pxOrElse { Int.MAX_VALUE }
        val outputHeight = size.height.pxOrElse { Int.MAX_VALUE }
        val outputSize = minOf(outputWidth, outputHeight)
        val output = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)

        val rect = RectF(
            /* left = */ 0f,
            /* top = */ 0f,
            /* right = */ outputWidth.toFloat(),
            /* bottom = */ outputHeight.toFloat()
        )
        val matrix = Matrix().apply {
            setTranslate(
                /* dx = */ (outputWidth - input.width) * .5f,
                /* dy = */ (outputHeight - input.height) * .5f
            )
        }
        val bitmapShader = BitmapShader(
            /* bitmap = */ input,
            /* tileX = */ Shader.TileMode.DECAL,
            /* tileY = */ Shader.TileMode.DECAL
        ).apply {
            setLocalMatrix(matrix)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            shader = bitmapShader
        }

        return output.applyCanvas {
            drawRoundRect(
                /* rect = */ rect,
                /* rx = */ radiusPx,
                /* ry = */ radiusPx,
                /* paint = */ paint
            )
        }
    }
}