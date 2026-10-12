package com.tangem.features.feed.rwa.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.markets.tokenselector.TokenSelectorSectionUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class RwaSearchUM(
    val query: String,
    val portfolio: RwaSearchPortfolioUM,
    val list: RwaSearchListUM,
)

@Immutable
internal sealed interface RwaSearchPortfolioUM {

    /** Nothing matched — the section, header included, is not rendered at all. */
    data object Empty : RwaSearchPortfolioUM

    /** Wallet/account sections, rendered inline rather than behind a nested bottom sheet. */
    data class Content(val sections: ImmutableList<TokenSelectorSectionUM>) : RwaSearchPortfolioUM
}

@Immutable
internal sealed interface RwaSearchListUM {

    data object Loading : RwaSearchListUM

    data class Content(
        val items: ImmutableList<RwaListItemUM>,
        val loadMore: () -> Unit,
    ) : RwaSearchListUM

    /**
     * The search finished and matched nothing.
     *
     * Stays a `data object` on purpose: the model's `distinctUntilChanged` is what suppresses a repeated
     * not-found snapshot, and a payload here would defeat it.
     */
    data object NotFound : RwaSearchListUM

    data class Error(val onRetry: () -> Unit) : RwaSearchListUM
}