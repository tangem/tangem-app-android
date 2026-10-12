package com.tangem.features.feed.crypto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.crypto.ui.state.MarketIndexStatus

enum class IndexScaleIndicatorSize {
    Small,
    Big,
    ;

    val height: Dp
        get() = when (this) {
            Small -> 12.dp
            Big -> 16.dp
        }

    val dotHeight: Dp
        get() = when (this) {
            Small -> 8.dp
            Big -> 10.dp
        }

    val barHeight: Dp
        get() = when (this) {
            Small -> 4.dp
            Big -> 6.dp
        }
}

/** Gradient scale with a thumb positioned at [progress] (`0f..1f`). */
@Composable
internal fun IndexScaleIndicator(progress: Float, size: IndexScaleIndicatorSize, modifier: Modifier = Modifier) {
    val accent = TangemTheme.colors3.text.accent
    Box(
        modifier = modifier.height(height = size.height),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(size.barHeight)
                .clip(shape = CircleShape)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            accent.red,
                            accent.orange,
                            accent.yellow,
                            accent.green,
                            accent.blue,
                            accent.blue,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(
                    alignment = BiasAlignment(
                        horizontalBias = progress.coerceIn(0f, 1f) * 2f - 1f,
                        verticalBias = 0f,
                    ),
                )
                .size(size = size.dotHeight)
                .clip(CircleShape)
                .background(TangemTheme.colors3.icon.primary),
        )
    }
}

/** Colour the reading's label is shown in, both on the card and in its info sheet. */
@Composable
@ReadOnlyComposable
internal fun MarketIndexStatus.textColor(): Color = when (this) {
    MarketIndexStatus.Error -> TangemTheme.colors3.text.status.error
    MarketIndexStatus.Warning -> TangemTheme.colors3.text.status.warning
    MarketIndexStatus.Success -> TangemTheme.colors3.text.status.success
    MarketIndexStatus.Info -> TangemTheme.colors3.text.status.info
}