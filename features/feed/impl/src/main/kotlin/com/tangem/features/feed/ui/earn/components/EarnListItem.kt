package com.tangem.features.feed.ui.earn.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.feed.ui.earn.state.EarnListItemUM

@Composable
internal fun EarnListItem(item: EarnListItemUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RectangleShape)
            .clickable { item.onItemClick() }
            .padding(
                horizontal = 12.dp,
                vertical = 15.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CurrencyIcon(
            modifier = Modifier.size(36.dp),
            state = item.currencyIconState,
            shouldDisplayNetwork = true,
            networkBadgeBackground = TangemTheme.colors.background.action,
        )

        SpacerW(12.dp)

        Column(modifier = Modifier.weight(1f)) {
            TokenTitle(
                name = item.tokenName.resolveReference(),
                symbol = item.symbol.resolveReference(),
            )
            SpacerH(2.dp)
            Text(
                text = item.network.resolveReference(),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.caption2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        SpacerW(8.dp)

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = item.earnValue.resolveReference(),
                color = TangemTheme.colors.text.accent,
                style = TangemTheme.typography.body2,
                maxLines = 1,
            )
            SpacerH(2.dp)
            Text(
                text = item.earnType.resolveReference(),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.caption2,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TokenTitle(name: String, symbol: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Text(
            modifier = Modifier
                .weight(1f, fill = false)
                .alignByBaseline(),
            text = name,
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.subtitle2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SpacerW(4.dp)
        Text(
            modifier = Modifier.alignByBaseline(),
            text = symbol,
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.body2,
            maxLines = 1,
            overflow = TextOverflow.Visible,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnListItemPreview() {
    TangemThemePreview {
        Column(modifier = Modifier.background(TangemTheme.colors.background.primary)) {
            EarnListItem(
                item = EarnListItemUM(
                    network = stringReference("Ethereum"),
                    symbol = stringReference("ETH"),
                    tokenName = stringReference("Ethereum"),
                    currencyIconState = CurrencyIconState.TokenIcon(
                        url = null,
                        topBadgeIconResId = R.drawable.img_eth_22,
                        fallbackTint = TangemColorPalette.Black,
                        fallbackBackground = TangemColorPalette.Meadow,
                        isGrayscale = false,
                        shouldShowCustomBadge = false,
                    ),
                    earnValue = stringReference("APY 8.50%"),
                    earnType = stringReference("Yield"),
                    onItemClick = {},
                ),
            )
            EarnListItem(
                item = EarnListItemUM(
                    network = stringReference("Ethereum"),
                    symbol = stringReference("USDT"),
                    tokenName = stringReference("Tether"),
                    currencyIconState = CurrencyIconState.TokenIcon(
                        url = null,
                        topBadgeIconResId = R.drawable.img_eth_22,
                        fallbackTint = TangemColorPalette.Black,
                        fallbackBackground = TangemColorPalette.Meadow,
                        isGrayscale = false,
                        shouldShowCustomBadge = false,
                    ),
                    earnValue = stringReference("APY 8.50%"),
                    earnType = stringReference("Yield"),
                    onItemClick = {},
                ),
            )
        }
    }
}