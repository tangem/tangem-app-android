package com.tangem.tap.features.home.compose.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.SpacerW8
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonColors
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.wallet.R

@Suppress("MagicNumber")
@Composable
internal fun HomeButtons(
    isDarkBackground: Boolean,
    btnScanStateInProgress: Boolean,
    onScanButtonClick: () -> Unit,
    onShopButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier,
    ) {
        ScanCardButton(
            modifier = Modifier.weight(weight = 1f),
            isDarkBackground = isDarkBackground,
            showProgress = btnScanStateInProgress,
            onClick = onScanButtonClick,
        )
        SpacerW8()
        OrderCardButton(
            modifier = Modifier.weight(weight = 1f),
            isDarkBackground = isDarkBackground,
            onClick = onShopButtonClick,
        )
    }
}

@Composable
private fun ScanCardButton(
    isDarkBackground: Boolean,
    showProgress: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemButton(
        modifier = modifier,
        text = stringResource(id = R.string.home_button_scan),
        icon = TangemButtonIconPosition.End(iconResId = R.drawable.ic_tangem_24),
        colors = if (isDarkBackground) DarkBgScanCardButtonColors else LightBgScanCardButtonColors,
        showProgress = showProgress,
        enabled = true,
        onClick = onClick,
    )
}

@Composable
private fun OrderCardButton(isDarkBackground: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemButton(
        modifier = modifier,
        text = stringResource(id = R.string.home_button_order),
        icon = TangemButtonIconPosition.None,
        colors = if (isDarkBackground) DarkBgOrderCardButtonColors else LightBgOrderCardButtonColors,
        showProgress = false,
        enabled = true,
        onClick = onClick,
    )
}

private val LightBgScanCardButtonColors: ButtonColors = TangemButtonColors(
    backgroundColor = TangemColorPalette.Light2,
    contentColor = TangemColorPalette.Dark6,
    disabledBackgroundColor = TangemColorPalette.Light2,
    disabledContentColor = TangemColorPalette.Dark6,
)

private val DarkBgScanCardButtonColors: ButtonColors = TangemButtonColors(
    backgroundColor = TangemColorPalette.Dark5,
    contentColor = TangemColorPalette.White,
    disabledBackgroundColor = TangemColorPalette.Dark5,
    disabledContentColor = TangemColorPalette.White,
)

private val LightBgOrderCardButtonColors: ButtonColors = TangemButtonColors(
    backgroundColor = TangemColorPalette.Dark6,
    contentColor = TangemColorPalette.White,
    disabledBackgroundColor = TangemColorPalette.Dark6,
    disabledContentColor = TangemColorPalette.White,
)

private val DarkBgOrderCardButtonColors: ButtonColors = TangemButtonColors(
    backgroundColor = TangemColorPalette.Light1,
    contentColor = TangemColorPalette.Dark6,
    disabledBackgroundColor = TangemColorPalette.Light1,
    disabledContentColor = TangemColorPalette.Dark6,
)

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun HomeButtonsPreview(@PreviewParameter(HomeButtonsParameterProvider::class) state: HomeButtonsState) {
    TangemTheme {
        Box(
            modifier = Modifier.background(if (state.isDarkBackground) Color.Black else Color.White),
        ) {
            HomeButtons(
                modifier = Modifier.padding(all = TangemTheme.dimens.spacing16),
                isDarkBackground = state.isDarkBackground,
                btnScanStateInProgress = state.btnScanStateInProgress,
                onScanButtonClick = {},
                onShopButtonClick = {},
            )
        }
    }
}

private class HomeButtonsParameterProvider : CollectionPreviewParameterProvider<HomeButtonsState>(
    collection = listOf(
        HomeButtonsState(
            isDarkBackground = false,
            btnScanStateInProgress = false,
        ),
        HomeButtonsState(
            isDarkBackground = true,
            btnScanStateInProgress = false,
        ),
        HomeButtonsState(
            isDarkBackground = false,
            btnScanStateInProgress = true,
        ),
        HomeButtonsState(
            isDarkBackground = true,
            btnScanStateInProgress = true,
        ),
    ),
)

private data class HomeButtonsState(
    val isDarkBackground: Boolean,
    val btnScanStateInProgress: Boolean,
)
// endregion Preview