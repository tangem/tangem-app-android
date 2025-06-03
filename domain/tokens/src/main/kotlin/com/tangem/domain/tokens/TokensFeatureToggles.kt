package com.tangem.domain.tokens

/**
 * Tokens feature toggles
 *
[REDACTED_AUTHOR]
 */
interface TokensFeatureToggles {

    val isNetworksLoadingRefactoringEnabled: Boolean

    val isQuotesLoadingRefactoringEnabled: Boolean

    val isStakingLoadingRefactoringEnabled: Boolean
}