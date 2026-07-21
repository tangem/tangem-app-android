package com.tangem.features.feed.ui.earn.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.models.earn.EarnType
import com.tangem.features.feed.ui.earn.state.EarnListItemUM

@Composable
internal fun MostlyUsedCard(item: EarnListItemUM, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(178.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.secondary)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        CurrencyIcon(
            state = item.currencyIconState,
            shouldDisplayNetwork = true,
            networkBadgeSize = 16.dp,
            iconSize = 40.dp,
            networkBadgeBackground = TangemTheme.colors3.bg.secondary,
        )

        SpacerH(22.dp)

        Row(
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                modifier = Modifier.weight(weight = 1f, fill = false),
                text = item.tokenName.resolveReference(),
                color = TangemTheme.colors3.text.primary,
                style = TangemTheme.typography3.body.medium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            SpacerW(4.dp)
            Text(
                text = item.symbol.resolveReference(),
                color = TangemTheme.colors3.text.secondary,
                style = TangemTheme.typography3.caption.medium,
                maxLines = 1,
            )
        }

        SpacerH(2.dp)

        Text(
            text = item.earnValue.resolveReference(),
            color = TangemTheme.colors3.text.accent.green,
            style = TangemTheme.typography3.caption.medium,
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun EarnListItemPreview() {
    TangemThemePreviewRedesign {
        MostlyUsedCard(
            EarnListItemUM(
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
                earnValue = stringReference("APY 6.54%"),
                earnTypeTitle = stringReference("Yield"),
                earnType = EarnType.YIELD,
                onItemClick = {},
            ),
            onClick = {},
        )
    }
}