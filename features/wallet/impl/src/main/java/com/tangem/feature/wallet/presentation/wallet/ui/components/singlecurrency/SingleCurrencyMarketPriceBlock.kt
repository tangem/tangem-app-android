package com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.marketprice.MarketPriceBlock
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState

/**
 * Single currency market price block
 *
 * @param state    component state
 * @param modifier modifier
 *
* [REDACTED_AUTHOR]
 */
internal fun LazyListScope.marketPriceBlock(state: MarketPriceBlockState, modifier: Modifier = Modifier) {
    item { MarketPriceBlock(state = state, modifier = modifier) }
}
