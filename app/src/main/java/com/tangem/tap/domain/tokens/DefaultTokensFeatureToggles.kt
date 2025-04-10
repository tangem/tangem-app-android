package com.tangem.tap.domain.tokens

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.tokens.TokensFeatureToggles

internal class DefaultTokensFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : TokensFeatureToggles {

    override val isNetworksLoadingRefactoringEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "NETWORKS_LOADING_REFACTORING_ENABLED")

    override val isQuotesLoadingRefactoringEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "QUOTES_LOADING_REFACTORING_ENABLED")

    override val isStakingLoadingRefactoringEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "STAKING_LOADING_REFACTORING_ENABLED")
}
