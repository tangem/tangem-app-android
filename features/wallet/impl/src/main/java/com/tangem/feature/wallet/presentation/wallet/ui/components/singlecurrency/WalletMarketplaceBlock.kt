package com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.common.state.PriceChangeConfig
import com.tangem.feature.wallet.presentation.wallet.state.WalletMarketplaceBlockState

/**
 * Wallet marketplace block
 *
 * @param state state
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun WalletMarketplaceBlock(state: WalletMarketplaceBlockState, modifier: Modifier = Modifier) {
    var rootWidth by remember { mutableStateOf(value = 0) }
    Column(
        modifier = modifier
            .background(
                color = TangemTheme.colors.background.primary,
                shape = RoundedCornerShape(TangemTheme.dimens.radius14),
            )
            .heightIn(min = TangemTheme.dimens.size70)
            .fillMaxWidth()
            .padding(all = TangemTheme.dimens.spacing14)
            .onSizeChanged { rootWidth = it.width },
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing6),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(id = R.string.wallet_marketplace_block_title, state.currencyName),
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.subtitle2,
        )

        when (state) {
            is WalletMarketplaceBlockState.Loading -> {
                RectangleShimmer(
                    modifier = Modifier.size(width = TangemTheme.dimens.size158, height = TangemTheme.dimens.size20),
                )
            }
            is WalletMarketplaceBlockState.Content -> {
                Price(
                    config = state,
                    priceWidthDp = with(LocalDensity.current) { rootWidth.div(other = 2).toDp() },
                )
            }
        }
    }
}

@Composable
private fun Price(config: WalletMarketplaceBlockState.Content, priceWidthDp: Dp) {
    Row(
        modifier = Modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing8),
    ) {
        Text(
            text = config.price,
            modifier = Modifier.widthIn(max = priceWidthDp),
            color = TangemTheme.colors.text.primary1,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = TangemTheme.typography.body2,
        )

        PriceChangeInPercent(config.priceChangeConfig)

        Text(
            text = stringResource(id = R.string.wallet_marketprice_block_update_time),
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.body2,
        )
    }
}

@Composable
private fun PriceChangeInPercent(config: PriceChangeConfig) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing4),
    ) {
        Image(
            painter = painterResource(
                id = when (config.type) {
                    PriceChangeConfig.Type.UP -> R.drawable.img_arrow_up_8
                    PriceChangeConfig.Type.DOWN -> R.drawable.img_arrow_down_8
                },
            ),
            contentDescription = null,
        )

        Text(
            text = config.valueInPercent,
            color = when (config.type) {
                PriceChangeConfig.Type.UP -> TangemTheme.colors.text.accent
                PriceChangeConfig.Type.DOWN -> TangemTheme.colors.text.warning
            },
            style = TangemTheme.typography.body2,
        )
    }
}

@Preview
@Composable
private fun Preview_MarketplaceBlock_Light(
    @PreviewParameter(WalletMarketplaceStateProvider::class)
    state: WalletMarketplaceBlockState,
) {
    TangemTheme(isDark = false) {
        WalletMarketplaceBlock(state = state)
    }
}

@Preview
@Composable
private fun Preview_MarketplaceBlock_Dark(
    @PreviewParameter(WalletMarketplaceStateProvider::class)
    state: WalletMarketplaceBlockState,
) {
    TangemTheme(isDark = true) {
        WalletMarketplaceBlock(state = state)
    }
}

private class WalletMarketplaceStateProvider : CollectionPreviewParameterProvider<WalletMarketplaceBlockState>(
    collection = listOf(
        WalletPreviewData.marketplaceBlockContent,
        WalletPreviewData.marketplaceBlockContent.copy(
            priceChangeConfig = PriceChangeConfig(
                valueInPercent = "5.16%",
                type = PriceChangeConfig.Type.DOWN,
            ),
        ),
        WalletMarketplaceBlockState.Loading(currencyName = "BTC"),
    ),
)