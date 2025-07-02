package com.tangem.domain.tokens

/**
 * Tokens feature toggles
 *
[REDACTED_AUTHOR]
 */
interface TokensFeatureToggles {

    val isStakingLoadingRefactoringEnabled: Boolean

    val isWalletBalanceFetcherEnabled: Boolean
}