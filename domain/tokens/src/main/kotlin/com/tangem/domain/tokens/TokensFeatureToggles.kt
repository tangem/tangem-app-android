package com.tangem.domain.tokens

/**
 * Tokens feature toggles
 *
[REDACTED_AUTHOR]
 */
interface TokensFeatureToggles {

    /**
     * Gates the migration of `CryptoCurrencyStatus` staking/yield balances to the generic
     * `BalanceContribution` summation path. Dormant until Phase 2 wires it into the total calculators.
     */
    val isBalanceContributionsEnabled: Boolean
}