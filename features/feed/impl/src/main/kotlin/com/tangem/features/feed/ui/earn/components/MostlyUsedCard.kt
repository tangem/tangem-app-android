package com.tangem.features.feed.ui.earn.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.tangem.core.ui.ds.opportunities.OpportunitiesBG
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.*
import com.tangem.features.feed.ui.earn.state.EarnListItemUM

@Composable
internal fun MostlyUsedCard(item: EarnListItemUM, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (LocalRedesignEnabled.current) {
        MostlyUsedCardV2(
            modifier = modifier,
            item = item,
            onClick = onClick,
        )
    } else {
        MostlyUsedCardV1(
            modifier = modifier,
            item = item,
            onClick = onClick,
        )
    }
}

@Composable
private fun MostlyUsedCardV2(item: EarnListItemUM, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OpportunitiesBG(
        modifier = modifier
            .width(148.dp)
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .clickable(onClick = onClick),
        icon = TangemIconUM.Currency(item.currencyIconState),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            CurrencyIcon(
                modifier = Modifier.size(32.dp),
                state = item.currencyIconState,
                shouldDisplayNetwork = true,
                networkBadgeSize = 12.dp,
                networkBadgeBackground = TangemTheme.colors.background.action,
            )

            SpacerH(22.dp)

            Row(
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    modifier = Modifier.weight(weight = 1f, fill = false),
                    text = item.tokenName.resolveReference(),
                    color = TangemTheme.colors.text.primary1,
                    style = TangemTheme.typography2.bodySemibold16,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                SpacerW(4.dp)
                Text(
                    text = item.symbol.resolveReference(),
                    color = TangemTheme.colors.text.tertiary,
                    style = TangemTheme.typography2.captionSemibold12,
                    maxLines = 1,
                )
            }

            SpacerH(2.dp)

            Text(
                text = item.earnValue.resolveReference(),
                color = TangemTheme.colors2.text.status.positive,
                style = TangemTheme.typography2.captionSemibold12,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MostlyUsedCardV1(item: EarnListItemUM, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(148.dp)
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        CurrencyIcon(
            modifier = Modifier.size(32.dp),
            state = item.currencyIconState,
            shouldDisplayNetwork = true,
            networkBadgeSize = 12.dp,
            networkBadgeBackground = TangemTheme.colors.background.action,
        )

        SpacerH(8.dp)

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(weight = 1f, fill = false),
                text = item.tokenName.resolveReference(),
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.subtitle2,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            SpacerW(4.dp)
            Text(
                text = item.symbol.resolveReference(),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.subtitle2,
                maxLines = 1,
            )
        }

        SpacerH(2.dp)

        Text(
            text = item.earnValue.resolveReference(),
            color = TangemTheme.colors.text.accent,
            style = TangemTheme.typography.caption1,
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun EarnListItemPreviewV1() {
    TangemThemePreview {
        MostlyUsedCardV1(
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
                earnType = stringReference("Yield"),
                onItemClick = {},
            ),
            onClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun EarnListItemPreviewV2() {
    TangemThemePreviewRedesign {
        MostlyUsedCardV2(
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
                earnType = stringReference("Yield"),
                onItemClick = {},
            ),
            onClick = {},
        )
    }
}