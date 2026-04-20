package com.tangem.features.feed.ui.search.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.marketprice.PriceChangeInPercent
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.search.state.BalanceDisplayState
import com.tangem.features.feed.ui.search.state.UserAssetItemUM

@Composable
fun SingleUserAssetItem(shouldUsePriceBlock: Boolean, item: UserAssetItemUM.Single, modifier: Modifier = Modifier) {
    TangemRowContainer(
        modifier = modifier.clickable(onClick = item.onClick),
        content = {
            TangemIcon(
                modifier = Modifier
                    .layoutId(layoutId = TangemRowLayoutId.HEAD)
                    .size(40.dp)
                    .padding(end = TangemTheme.dimens2.x1),
                tangemIconUM = item.icon,
            )

            Text(
                modifier = Modifier.layoutId(TangemRowLayoutId.START_TOP),
                text = item.tokenName,
                style = TangemTheme.typography2.bodyMedium16,
                color = TangemTheme.colors2.text.neutral.primary,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )

            if (shouldUsePriceBlock) {
                PriceBlock(
                    modifier = Modifier.layoutId(TangemRowLayoutId.START_BOTTOM),
                    priceChangeState = item.priceChangeState,
                    fiatRate = item.fiatRate,
                    balanceState = item.balanceState,
                )
            } else {
                Text(
                    modifier = Modifier.layoutId(TangemRowLayoutId.START_BOTTOM),
                    text = item.networkName,
                    style = TangemTheme.typography2.captionMedium12,
                    color = TangemTheme.colors2.text.neutral.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                )
            }

            BalanceColumn(
                modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.END_TOP),
                balanceState = item.balanceState,
                isBalanceHidden = item.isBalanceHidden,
            )
        },
    )
}

@Composable
private fun PriceBlock(
    priceChangeState: PriceChangeState,
    fiatRate: String?,
    balanceState: BalanceDisplayState,
    modifier: Modifier = Modifier,
) {
    val isDisabled = remember(balanceState) {
        balanceState is BalanceDisplayState.Unreachable
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
    ) {
        if (fiatRate != null) {
            Text(
                text = fiatRate,
                style = TangemTheme.typography2.captionMedium12,
                color = if (isDisabled) {
                    TangemTheme.colors2.text.status.disabled
                } else {
                    TangemTheme.colors2.text.neutral.secondary
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        AnimatedContent(
            targetState = priceChangeState,
            contentKey = { it::class },
        ) { animatedState ->
            when (animatedState) {
                is PriceChangeState.Content -> {
                    PriceChangeInPercent(
                        valueInPercent = animatedState.valueInPercent,
                        type = animatedState.type,
                        textStyle = TangemTheme.typography2.captionMedium12,
                        isDisabled = isDisabled,
                    )
                }
                PriceChangeState.Unknown -> Unit
            }
        }
    }
}