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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.models.earn.EarnType
import com.tangem.features.feed.ui.earn.state.EarnListItemUM

@Composable
internal fun EarnListItem(item: EarnListItemUM, modifier: Modifier = Modifier) {
    TangemRowContainer(
        modifier = modifier.clickable { item.onItemClick() },
        contentPadding = PaddingValues(12.dp),
        content = {
            TangemIcon(
                tangemIconUM = TangemIconUM.Currency(item.currencyIconState),
                modifier = Modifier
                    .layoutId(layoutId = TangemRowLayoutId.HEAD)
                    .padding(end = 8.dp)
                    .size(40.dp),
            )

            TokenTitle(
                modifier = Modifier
                    .layoutId(layoutId = TangemRowLayoutId.START_TOP)
                    .padding(end = 8.dp),
                name = item.tokenName.resolveReference(),
                symbol = item.symbol.resolveReference(),
            )

            Text(
                modifier = Modifier
                    .layoutId(layoutId = TangemRowLayoutId.START_BOTTOM)
                    .padding(end = 8.dp),
                text = item.network.resolveReference(),
                color = TangemTheme.colors3.text.secondary,
                style = TangemTheme.typography3.caption.medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.END_TOP),
                text = item.earnValue.resolveReference(),
                color = TangemTheme.colors3.text.primary,
                style = TangemTheme.typography3.body.medium,
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
        Text(
            modifier = Modifier
                .weight(1f, fill = false)
                .alignByBaseline(),
            text = name,
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.body.medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SpacerW(4.dp)
        Text(
            modifier = Modifier.alignByBaseline(),
            text = symbol,
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.caption.medium,
            maxLines = 1,
            overflow = TextOverflow.Visible,
        )
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
            tint = TangemTheme.colors3.icon.secondary,
            contentDescription = null,
        )
        Text(
            text = earnTypeTitle.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnListItemPreview() {
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