package com.tangem.feature.qrscanning.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.IntOffset
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.TopAppBarButton
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
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
    var isFlash by remember { mutableStateOf(false) }

    BackHandler(onBack = uiState.onBackClick)

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        CameraView(
            executor = executor,
            analyzer = analyzer,
            isFlash = isFlash,
        )
        Overlay(
            message = uiState.message,
            pasteAction = uiState.pasteAction,
        )

        TangemTopAppBar(
            modifier = Modifier.statusBarsPadding(),
            title = uiState.topBarConfig.title?.resolveReference(),
            startButton = TopAppBarButtonUM(
                iconRes = uiState.topBarConfig.startIcon,
                onIconClicked = uiState.onBackClick,
            ),
            textColor = TangemTheme.colors.text.constantWhite,
            iconTint = TangemColorPalette.White,
            containerColor = Color.Transparent,
            endContent = {
                AnimatedContent(
                    targetState = isFlash,
                    transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                    label = "Flash Change",
                ) {
                    TopAppBarButton(
                        button = TopAppBarButtonUM(
                            iconRes = if (it) R.drawable.ic_flash_on_24 else R.drawable.ic_flash_off_24,
                            onIconClicked = {
                                isFlash = !isFlash
                            },
                        ),
                        tint = TangemColorPalette.White,
                    )
                }

                TopAppBarButton(
                    button = TopAppBarButtonUM(
                        iconRes = R.drawable.ic_gallery_24,
                        onIconClicked = uiState.onGalleryClick,
                    ),
                    tint = TangemColorPalette.White,
                )
            },
        )
        if (uiState.bottomSheetConfig != null) {
            CameraDeniedBottomSheet(uiState.bottomSheetConfig)
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun Overlay(
    message: TextReference?,
    pasteAction: PasteAction,
    overlayColor: Color = TangemColorPalette.Black.copy(alpha = 0.7f),
) {
    val whiteColor = TangemTheme.colors.text.constantWhite
    val borderWidth = with(LocalDensity.current) { TangemTheme.dimens.spacing4.toPx() }
    val borderLength = with(LocalDensity.current) { TangemTheme.dimens.size20.toPx() }

    val textMeasurer = rememberTextMeasurer()
    val textPadding = with(LocalDensity.current) { TangemTheme.dimens.spacing48.toPx() }
    val style = TangemTheme.typography.body1.copy(textAlign = TextAlign.Center)
    val text = message?.resolveReference()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val minDimension = minOf(this.constraints.minWidth, this.constraints.minHeight)
        val squareSize = minDimension * QR_CODE_SIZE
        val squareSizeInDp = with(LocalDensity.current) { squareSize.toDp() }
        val verticalOffset = (constraints.minHeight - squareSize) / 2f
        val horizontalOffset = (constraints.minWidth - squareSize) / 2f

        Canvas(modifier = Modifier.fillMaxSize()) {
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
                        y = verticalOffset - textPadding - textLayoutResult.size.height,
                    ),
                )
            }
        }
        if (pasteAction is PasteAction.Perform) {
            TangemButton(
                modifier = Modifier
                    .offset(
                        offset = {
                            IntOffset(
                                x = horizontalOffset.roundToInt(),
                                y = (verticalOffset + squareSize + textPadding).roundToInt(),
                            )
                        },
                    )
                    .width(squareSizeInDp),
                text = stringResourceSafe(id = R.string.wallet_connect_paste_from_clipboard),
                colors = TangemButtonsDefaults.defaultTextButtonColors.copy(contentColor = whiteColor),
                onClick = pasteAction.action,
                textStyle = TangemTheme.typography.subtitle1,
                icon = TangemButtonIconPosition.End(iconResId = R.drawable.ic_copy_24),
                showProgress = false,
                enabled = true,
            )
        }
    }
}