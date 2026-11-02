package com.tangem.domain.tokens

/**
 * Source type that is necessary to load cryptocurrency data
 *
[REDACTED_AUTHOR]
 */
enum class FetchingSource {
    NETWORK,
    QUOTE,
    /** Every registered [com.tangem.domain.models.currency.balance.BalanceContributionFetcher], e.g. staking. */
    CONTRIBUTIONS,
}