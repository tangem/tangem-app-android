package com.tangem.features.commonfeatures.api.choosetoken.model

/** Balance filter applied to the FROM token selector in Swap. Non-persisted; default depends on the feature. */
enum class BalanceFilter {
    All,
    HideZero,
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