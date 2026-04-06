package com.tangem.features.feed.ui.earn.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.*
import com.tangem.domain.models.earn.EarnType
import com.tangem.features.feed.ui.earn.state.EarnListItemUM

@Composable
internal fun EarnListItem(item: EarnListItemUM, modifier: Modifier = Modifier) {
    if (LocalRedesignEnabled.current) {
        EarnListItemV2(
            item = item,
            modifier = modifier,
        )
    } else {
        EarnListItemV1(
            item = item,
            modifier = modifier,
        )
    }
}

@Composable
private fun EarnListItemV1(item: EarnListItemUM, modifier: Modifier = Modifier) {
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
                text = item.earnTypeTitle.resolveReference(),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.caption2,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun EarnListItemV2(item: EarnListItemUM, modifier: Modifier = Modifier) {
    TangemRowContainer(
        modifier = modifier.clickable { item.onItemClick() },
        contentPadding = PaddingValues(12.dp),
        content = {
            TangemIcon(
                tangemIconUM = TangemIconUM.Currency(item.currencyIconState),
                modifier = Modifier
                    .layoutId(layoutId = TangemRowLayoutId.HEAD)
                    .padding(end = TangemTheme.dimens2.x2),
            )

            TokenTitle(
                modifier = Modifier
                    .layoutId(layoutId = TangemRowLayoutId.START_TOP)
                    .padding(end = TangemTheme.dimens2.x2),
                name = item.tokenName.resolveReference(),
                symbol = item.symbol.resolveReference(),
            )

            Text(
                modifier = Modifier
                    .layoutId(layoutId = TangemRowLayoutId.START_BOTTOM)
                    .padding(end = TangemTheme.dimens2.x2),
                text = item.network.resolveReference(),
                color = TangemTheme.colors2.text.neutral.secondary,
                style = TangemTheme.typography2.captionSemibold12,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.END_TOP),
                text = item.earnValue.resolveReference(),
                color = TangemTheme.colors2.text.neutral.primary,
                style = TangemTheme.typography2.bodySemibold16,
                maxLines = 1,
            )

            ModeBlock(
                modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.END_BOTTOM),
                earnType = item.earnType,
                earnTypeTitle = item.earnTypeTitle,
            )
        },
    )
}

@Composable
private fun TokenTitle(name: String, symbol: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        if (LocalRedesignEnabled.current) {
            Text(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .alignByBaseline(),
                text = name,
                color = TangemTheme.colors2.text.neutral.primary,
                style = TangemTheme.typography2.bodySemibold16,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            SpacerW(4.dp)
            Text(
                modifier = Modifier.alignByBaseline(),
                text = symbol,
                color = TangemTheme.colors2.text.neutral.secondary,
                style = TangemTheme.typography2.captionSemibold12,
                maxLines = 1,
                overflow = TextOverflow.Visible,
            )
        } else {
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
}

@Composable
private fun ModeBlock(earnType: EarnType, earnTypeTitle: TextReference, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(
                id = when (earnType) {
                    EarnType.STAKING -> R.drawable.ic_staking_new_16
                    EarnType.YIELD -> R.drawable.ic_yield_mode_16
                },
            ),
            tint = TangemTheme.colors2.markers.iconGray,
            contentDescription = null,
        )
        Text(
            text = earnTypeTitle.resolveReference(),
            style = TangemTheme.typography2.captionSemibold12,
            color = TangemTheme.colors2.text.neutral.tertiary,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnListItemPreviewV1() {
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
                    earnTypeTitle = stringReference("Yield"),
                    earnType = EarnType.YIELD,
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
                    earnTypeTitle = stringReference("Yield"),
                    earnType = EarnType.YIELD,
                    onItemClick = {},
                ),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnListItemPreviewV2() {
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.background(TangemTheme.colors.background.primary)) {
            SpacerH(12.dp)
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
                    earnTypeTitle = stringReference("Yield"),
                    earnType = EarnType.YIELD,
                    onItemClick = {},
                ),
            )
            SpacerH(12.dp)
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
                    earnTypeTitle = stringReference("Yield"),
                    earnType = EarnType.YIELD,
                    onItemClick = {},
                ),
            )
            SpacerH(12.dp)
        }
    }
}