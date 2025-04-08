package com.tangem.features.nft.collections.ui

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.nft.collections.entity.NFTSalePriceUM

@Composable
internal fun NFTSalePrice(state: NFTSalePriceUM, modifier: Modifier = Modifier) {
    when (state) {
        is NFTSalePriceUM.Content -> {
            Text(
                modifier = modifier,
                text = state.price,
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        is NFTSalePriceUM.Failed -> {
            Text(
                modifier = modifier,
                text = "",
                style = TangemTheme.typography.caption2,
            )
        }
        is NFTSalePriceUM.Loading -> {
            TextShimmer(
                modifier = modifier
                    .width(TangemTheme.dimens.size48),
                style = TangemTheme.typography.caption2,
            )
        }
    }
}