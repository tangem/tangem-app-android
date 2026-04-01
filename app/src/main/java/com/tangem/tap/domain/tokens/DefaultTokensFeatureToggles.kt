package com.tangem.tap.domain.tokens

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.tokens.TokensFeatureToggles

@Suppress("UnusedPrivateProperty")
internal class DefaultTokensFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : TokensFeatureToggles