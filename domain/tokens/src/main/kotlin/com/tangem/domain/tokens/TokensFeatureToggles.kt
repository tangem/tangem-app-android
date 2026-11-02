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
}