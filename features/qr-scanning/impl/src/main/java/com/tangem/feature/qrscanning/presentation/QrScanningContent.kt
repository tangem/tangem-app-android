package com.tangem.feature.qrscanning.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import com.tangem.core.ui.components.appbar.AppBarWithBackButtonAndIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.qrscanning.impl.R
import com.tangem.feature.qrscanning.inner.MLKitBarcodeAnalyzer
import java.util.concurrent.ExecutorService
import kotlin.math.roundToInt

private const val QR_CODE_SIZE = 0.8f

@Composable
internal fun QrScanningContent(
    executor: () -> ExecutorService,
    analyzer: () -> MLKitBarcodeAnalyzer,
    uiState: QrScanningState,
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        CameraView(
            executor = executor,
            analyzer = analyzer,
        )
        Overlay(
            message = uiState.message,
        )
        AppBarWithBackButtonAndIcon(
            onBackClick = uiState.onBackClick,
            iconRes = R.drawable.ic_gallery_24,
            onIconClick = uiState.onGalleryClicked,
            backgroundColor = Color.Transparent,
            modifier = Modifier.statusBarsPadding(),
        )
    }
}

@Composable
private fun Overlay(message: TextReference?, overlayColor: Color = TangemColorPalette.Black.copy(alpha = 0.7f)) {
    val whiteColor = TangemTheme.colors.text.constantWhite
    val borderWidth = with(LocalDensity.current) { TangemTheme.dimens.spacing4.toPx() }
    val borderLength = with(LocalDensity.current) { TangemTheme.dimens.size20.toPx() }

    val textMeasurer = rememberTextMeasurer()
    val textPadding = with(LocalDensity.current) { TangemTheme.dimens.spacing24.toPx() }
    val style = TangemTheme.typography.caption2.copy(textAlign = TextAlign.Center)
    val text = message?.resolveReference()

    Canvas(modifier = Modifier.fillMaxSize()) {
        val squareSize = size.minDimension * QR_CODE_SIZE
        val verticalOffset = (size.height - squareSize) / 2f
        val horizontalOffset = (size.width - squareSize) / 2f
        drawRect(
            color = overlayColor,
            topLeft = Offset.Zero,
            size = Size(size.width, verticalOffset),
        )
        drawRect(
            color = overlayColor,
            topLeft = Offset(0f, verticalOffset),
            size = Size(horizontalOffset, squareSize),
        )
        drawRect(
            color = overlayColor,
            topLeft = Offset(horizontalOffset + squareSize, verticalOffset),
            size = Size(horizontalOffset, squareSize),
        )
        drawRect(
            color = overlayColor,
            topLeft = Offset(0f, verticalOffset + squareSize),
            size = Size(size.width, verticalOffset),
        )
        drawRect(
            color = whiteColor,
            topLeft = Offset(horizontalOffset, verticalOffset),
            size = Size(squareSize, squareSize),
            style = Stroke(
                width = borderWidth,
                join = StrokeJoin.Round,
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(2 * borderLength, squareSize - 2 * borderLength),
                    phase = borderLength,
                ),
            ),
        )
        text?.let {
            val textLayoutResult = text.let {
                textMeasurer.measure(
                    text = it,
                    style = style,
                    constraints = Constraints.fixedWidth(squareSize.roundToInt()),
                )
            }
            drawText(
                textLayoutResult = textLayoutResult,
                color = whiteColor,
                topLeft = Offset(
                    x = horizontalOffset,
                    y = verticalOffset + squareSize + textPadding,
                ),
            )
        }
    }
}