package com.tangem.domain.tokens

/**
 * Tokens feature toggles
 *
[REDACTED_AUTHOR]
 */
interface TokensFeatureToggles {

    /**
     * Balance sources (staking, networks, NFT) are fetched and cleaned through the registered
     * [com.tangem.domain.models.currency.balance.BalanceContributionFetcher] and
     * [com.tangem.domain.common.wallets.CryptoCurrencyDataCleaner] sets instead of hard-wired calls.
     */
    val isPluggableBalanceSourcesEnabled: Boolean

    /**
     * Yield supply status comes from the Yield Module positions source instead of the network status snapshot.
     *
     * Implies [isPluggableBalanceSourcesEnabled]: the positions are fetched only through the registered
     * [com.tangem.domain.models.currency.balance.BalanceContributionFetcher] set, so without it every yield token
     * would read as inactive.
     */
    val isYieldContributionEnabled: Boolean
}