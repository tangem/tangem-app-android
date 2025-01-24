package com.tangem.features.onramp.hottokens.portfolio.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.TangemTopAppBarHeight
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onramp.hottokens.portfolio.entity.OnrampAddToPortfolioUM
import com.tangem.features.onramp.impl.R

/**
 * Onramp add to portfolio content
 *
 * @param state state
 */
@Composable
fun OnrampAddToPortfolioContent(state: OnrampAddToPortfolioUM) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopAppBar(text = state.bsTitle)

        SpacerH(height = 19.dp)

        CurrencyIcon(state = state.currencyIconState)

        SpacerH12()

        CurrencyName(
            text = state.currencyName,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        SpacerH(height = 6.dp)

        CurrencyNetworkName(
            text = state.subtitle,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        SpacerH24()

        AddToPortfolioButton(
            text = state.addButtonText,
            onClick = state.onAddClick,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        SpacerH16()
    }
}

@Composable
private fun TopAppBar(text: TextReference) {
    TangemTopAppBar(
        title = text.resolveReference(),
        endButton = TopAppBarButtonUM(
            iconRes = R.drawable.ic_information_24,
            onIconClicked = {},
        ),
        iconTint = TangemTheme.colors.icon.informative,
        titleAlignment = Alignment.CenterHorizontally,
        height = TangemTopAppBarHeight.BOTTOM_SHEET,
    )
}

@Composable
private fun CurrencyName(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = TangemTheme.colors.text.primary1,
        maxLines = 1,
        style = TangemTheme.typography.subtitle1,
    )
}

@Composable
private fun CurrencyNetworkName(text: TextReference, modifier: Modifier = Modifier) {
    Text(
        text = text.resolveReference(),
        modifier = modifier,
        color = TangemTheme.colors.text.tertiary,
        maxLines = 1,
        style = TangemTheme.typography.body2,
    )
}

@Composable
private fun AddToPortfolioButton(text: TextReference, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemButton(
        text = text.resolveReference(),
        icon = TangemButtonIconPosition.End(iconResId = R.drawable.ic_tangem_24),
        onClick = onClick,
        colors = TangemButtonsDefaults.primaryButtonColors,
        showProgress = false,
        enabled = true,
        modifier = modifier.fillMaxWidth(),
        size = TangemButtonSize.WideAction,
        textStyle = TangemTheme.typography.subtitle1,
    )
}

@Preview
@Composable
private fun PreviewOnrampAddToPortfolioContent() {
    TangemThemePreview {
        val bottomSheetConfig = remember {
            TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = TangemBottomSheetConfigContent.Empty,
            )
        }
        OnrampAddToPortfolioBottomSheet(
            config = bottomSheetConfig,
            content = {
                OnrampAddToPortfolioContent(
                    state = OnrampAddToPortfolioUM(
                        currencyName = "Tether",
                        networkName = "Ethereum",
                        currencyIconState = CurrencyIconState.TokenIcon(
                            url = null,
                            topBadgeIconResId = R.drawable.img_eth_22,
                            fallbackTint = TangemColorPalette.Black,
                            fallbackBackground = TangemColorPalette.Meadow,
                            isGrayscale = false,
                            showCustomBadge = false,
                        ),
                        onAddClick = {},
                    ),
                )
            },
        )
    }
}