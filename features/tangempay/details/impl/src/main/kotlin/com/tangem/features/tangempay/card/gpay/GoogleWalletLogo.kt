@file:Suppress("MagicNumber", "NamedArguments")

package com.tangem.features.tangempay.card.gpay

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val GoogleGreen = Color(0xFF34A853)
private val GoogleYellow = Color(0xFFFBBC04)
private val GoogleRed = Color(0xFFEA4335)
private val GoogleBlue = Color(0xFF4285F4)

internal val GoogleWalletLogo: ImageVector by lazy {
    ImageVector.Builder(
        name = "GoogleWalletLogo",
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f,
    ).apply {
        path(fill = SolidColor(GoogleGreen), pathFillType = PathFillType.EvenOdd) {
            moveTo(18.4097f, 8.07533f)
            verticalLineTo(5.52422f)
            curveTo(18.4097f, 4.05086f, 17.2092f, 2.85034f, 15.7359f, 2.85034f)
            horizontalLineTo(4.26274f)
            curveTo(2.78938f, 2.86399f, 1.58887f, 4.0645f, 1.58887f, 5.52422f)
            verticalLineTo(8.07533f)
            curveTo(1.58887f, 8.33451f, 1.7935f, 8.53913f, 2.0527f, 8.53913f)
            horizontalLineTo(17.9459f)
            curveTo(18.2051f, 8.53913f, 18.4097f, 8.33451f, 18.4097f, 8.07533f)
            close()
        }
        path(fill = SolidColor(GoogleYellow), pathFillType = PathFillType.EvenOdd) {
            moveTo(18.4097f, 10.3124f)
            verticalLineTo(7.77493f)
            curveTo(18.4097f, 6.30159f, 17.2092f, 5.10107f, 15.7359f, 5.10107f)
            horizontalLineTo(4.26274f)
            curveTo(2.78938f, 5.10107f, 1.58887f, 6.30159f, 1.58887f, 7.77493f)
            verticalLineTo(10.3261f)
            curveTo(1.58887f, 10.5853f, 1.7935f, 10.7899f, 2.0527f, 10.7899f)
            horizontalLineTo(17.9459f)
            curveTo(18.2051f, 10.7762f, 18.4097f, 10.5716f, 18.4097f, 10.3124f)
            close()
        }
        path(fill = SolidColor(GoogleRed), pathFillType = PathFillType.EvenOdd) {
            moveTo(18.4097f, 12.5641f)
            verticalLineTo(10.013f)
            curveTo(18.4097f, 8.53963f, 17.2092f, 7.33911f, 15.7359f, 7.33911f)
            horizontalLineTo(4.26274f)
            curveTo(2.78938f, 7.33911f, 1.58887f, 8.53963f, 1.58887f, 10.013f)
            verticalLineTo(12.5641f)
            curveTo(1.58887f, 12.8232f, 1.7935f, 13.0279f, 2.0527f, 13.0279f)
            horizontalLineTo(17.9459f)
            curveTo(18.2051f, 13.0143f, 18.4097f, 12.8096f, 18.4097f, 12.5641f)
            close()
        }
        path(fill = SolidColor(GoogleBlue), pathFillType = PathFillType.EvenOdd) {
            moveTo(12.2844f, 11.3356f)
            lineTo(1.58887f, 8.85279f)
            verticalLineTo(14.4734f)
            curveTo(1.58887f, 15.9467f, 2.78938f, 17.1473f, 4.26274f, 17.1473f)
            horizontalLineTo(15.7359f)
            curveTo(17.2092f, 17.1473f, 18.4097f, 15.9467f, 18.4097f, 14.4734f)
            verticalLineTo(8.74365f)
            lineTo(15.8586f, 10.599f)
            curveTo(14.8218f, 11.3493f, 13.5258f, 11.6221f, 12.2844f, 11.3356f)
            close()
        }
    }.build()
}