package com.tangem.features.commonfeatures.api.choosetoken.model

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.commonfeatures.api.R

/** Balance filter applied to the FROM token selector in Swap. Non-persisted; default depends on the feature. */
enum class BalanceFilter(val label: TextReference) {
    All(resourceReference(R.string.common_all)),
    HideZero(resourceReference(R.string.swap_token_selector_filter_hide_zero_balance)),
}

/** Why a wallet's token list rendered empty — drives the empty-state UI. */
enum class EmptyReason {
    /** The wallet genuinely has no matching tokens. */
    NoTokens,

    /** Tokens exist but were all hidden by the zero-balance filter. Offers a "See all" reset. */
    FilteredOut,
}

/** UI model for the header balance-filter dropdown. Present only when the filter is enabled for the feature. */
data class BalanceFilterUM(
    val selected: BalanceFilter,
    val onOptionSelected: (BalanceFilter) -> Unit,
)