package com.tangem.features.feed.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

private const val MIN_STACKED_ICON_COUNT = 1
private const val MAX_STACKED_ICON_COUNT = 3
private const val FIRST_BACK_LAYER_HORIZONTAL_OFFSET_MULTIPLIER = 1
private const val SECOND_BACK_LAYER_HORIZONTAL_OFFSET_MULTIPLIER = 2
private const val MIN_STACKED_COUNT_FOR_FIRST_BACK_LAYER = 2
private const val FIRST_BACK_LAYER_ICON_ALPHA = 0.4f
private const val SECOND_BACK_LAYER_ICON_ALPHA = 0.2f

@Composable
fun LayeringIcons(
    tangemIconUM: TangemIconUM,
    modifier: Modifier = Modifier,
    count: Int = MIN_STACKED_ICON_COUNT,
    layerHorizontalShift: Dp = TangemTheme.dimens2.x1,
    iconSize: Dp = TangemTheme.dimens2.x10,
) {
    require(count > 0)

    val stackTrailingWidth = layerHorizontalShift * SECOND_BACK_LAYER_HORIZONTAL_OFFSET_MULTIPLIER

    Box(
        modifier = modifier.size(
            width = iconSize + stackTrailingWidth,
            height = iconSize,
        ),
    ) {
        val baseIconModifier = Modifier
            .align(Alignment.TopStart)
            .size(iconSize)

        if (count >= MAX_STACKED_ICON_COUNT) {
            TangemIcon(
                tangemIconUM = tangemIconUM,
                modifier = baseIconModifier
                    .offset(x = layerHorizontalShift * SECOND_BACK_LAYER_HORIZONTAL_OFFSET_MULTIPLIER)
                    .alpha(SECOND_BACK_LAYER_ICON_ALPHA),
            )
        }
        if (count >= MIN_STACKED_COUNT_FOR_FIRST_BACK_LAYER) {
            TangemIcon(
                tangemIconUM = tangemIconUM,
                modifier = baseIconModifier
                    .offset(x = layerHorizontalShift * FIRST_BACK_LAYER_HORIZONTAL_OFFSET_MULTIPLIER)
                    .alpha(FIRST_BACK_LAYER_ICON_ALPHA),
            )
        }
        TangemIcon(
            tangemIconUM = tangemIconUM,
            modifier = baseIconModifier,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun LayeringIconsPreview() {
    val previewCurrencyIcon = TangemIconUM.Currency(
        currencyIconState = CurrencyIconState.TokenIcon(
            url = null,
            topBadgeIconResId = null,
            fallbackTint = TangemColorPalette.Black,
            fallbackBackground = TangemColorPalette.Meadow,
            isGrayscale = false,
            shouldShowCustomBadge = false,
        ),
    )
    TangemThemePreview {
        Column(horizontalAlignment = Alignment.End) {
            LayeringIcons(count = MIN_STACKED_ICON_COUNT, tangemIconUM = previewCurrencyIcon)
            LayeringIcons(
                count = MAX_STACKED_ICON_COUNT,
                tangemIconUM = previewCurrencyIcon,
            )
            LayeringIcons(
                count = MIN_STACKED_COUNT_FOR_FIRST_BACK_LAYER,
                tangemIconUM = previewCurrencyIcon,
            )
        }
    }
}