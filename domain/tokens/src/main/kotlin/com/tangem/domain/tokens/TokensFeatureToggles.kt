package com.tangem.domain.tokens

/**
 * Tokens feature toggles
 *
[REDACTED_AUTHOR]
 */
interface TokensFeatureToggles {

    /** Yield supply status comes from the Yield Module status contribution instead of the network status snapshot. */
    val isYieldContributionEnabled: Boolean
}