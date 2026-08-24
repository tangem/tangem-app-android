package com.tangem.features.feed.earn.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.feed.earn.ui.state.EarnOpportunitiesItemUM

@Composable
internal fun OpportunitiesCard(item: EarnOpportunitiesItemUM, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(178.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.opaque.primary)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        CurrencyIcon(
            state = item.currencyIconState,
            shouldDisplayNetwork = true,
            networkBadgeSize = 16.dp,
            iconSize = 40.dp,
            networkBadgeBackground = TangemTheme.colors3.bg.opaque.primary,
        )

        SpacerH(22.dp)

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                modifier = Modifier.weight(weight = 1f, fill = false),
                text = item.tokenName.resolveReference(),
                color = TangemTheme.colors3.text.primary,
                style = TangemTheme.typography3.body.medium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            Text(
                text = item.symbol.resolveReference(),
                color = TangemTheme.colors3.text.secondary,
                style = TangemTheme.typography3.caption.medium,
                lineHeight = (TangemTheme.typography3.caption.medium.lineHeight.value + 1).sp,
                maxLines = 1,
            )
        }

        SpacerH(2.dp)

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.earnValue.resolveReference(),
                color = TangemTheme.colors3.text.accent.green,
                style = TangemTheme.typography3.caption.medium,
                maxLines = 1,
            )
            Text(
                text = item.earnType.resolveReference(),
                color = TangemTheme.colors3.text.secondary,
                style = TangemTheme.typography3.caption.medium,
                maxLines = 1,
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OpportunitiesCardPreview() {
    TangemThemePreviewRedesign {
        OpportunitiesCard(
            EarnOpportunitiesItemUM(
                id = "tether_YIELD",
                currencyIconState = CurrencyIconState.TokenIcon(
                    url = null,
                    topBadgeIconResId = R.drawable.img_eth_22,
                    fallbackTint = TangemColorPalette.Black,
                    fallbackBackground = TangemColorPalette.Meadow,
                    isGrayscale = false,
                    shouldShowCustomBadge = false,
                ),
                tokenName = stringReference("Tether"),
                symbol = stringReference("USDT"),
                earnValue = stringReference("APY 6.54%"),
                earnType = stringReference("Staking"),
                onItemClick = {},
            ),
            onClick = {},
        )
    }
}