package com.tangem.features.feed.earn.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.markets.tokenselector.TokenSelectorSectionUM
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class EarnSearchUM(
    val query: String,
    val portfolio: EarnSearchPortfolioUM,
    val earn: EarnSearchListUM,
)

@Immutable
internal sealed interface EarnSearchPortfolioUM {

    /** Nothing matched — the section, header included, is not rendered at all. */
    data object Empty : EarnSearchPortfolioUM

    /** Wallet/account sections, rendered inline rather than behind a nested bottom sheet. */
    data class Content(val sections: ImmutableList<TokenSelectorSectionUM>) : EarnSearchPortfolioUM
}

@Immutable
internal sealed interface EarnSearchListUM {

    data object Loading : EarnSearchListUM

    /**
     * The whole catalogue is already in memory, so there is nothing to paginate — the list carries
     * every match at once.
     */
    data class Content(val items: ImmutableList<TangemTokenRow.State.Content>) : EarnSearchListUM

    /** The catalogue loaded and matched nothing. */
    data object NotFound : EarnSearchListUM

    data class Error(val onRetryClicked: () -> Unit) : EarnSearchListUM
}