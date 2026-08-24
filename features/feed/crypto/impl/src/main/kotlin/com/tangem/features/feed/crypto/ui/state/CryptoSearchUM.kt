package com.tangem.features.feed.crypto.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.markets.tokenselector.TokenSelectorSectionUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class CryptoSearchUM(
    val query: String,
    val portfolio: PortfolioSearchUM,
    val market: MarketSearchUM,
)

@Immutable
internal sealed interface PortfolioSearchUM {

    /** Nothing matched — the section, header included, is not rendered at all. */
    data object Empty : PortfolioSearchUM

    /** Wallet/account sections, rendered inline rather than behind a nested bottom sheet. */
    data class Content(val sections: ImmutableList<TokenSelectorSectionUM>) : PortfolioSearchUM
}

@Immutable
internal sealed interface MarketSearchUM {

    data object Loading : MarketSearchUM

    data class Content(
        val items: ImmutableList<MarketPulseItemUM>,
        val loadMore: () -> Unit,
        val shouldShowUnderMarketCapLimitNotification: Boolean = false,
        val onShowUnderMarketCapLimitClick: () -> Unit = {},
    ) : MarketSearchUM

    /** The search finished and matched nothing. */
    data object NotFound : MarketSearchUM

    data class Error(val onRetry: () -> Unit) : MarketSearchUM
}