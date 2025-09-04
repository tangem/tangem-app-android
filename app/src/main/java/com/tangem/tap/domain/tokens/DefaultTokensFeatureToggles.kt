package com.tangem.tap.domain.tokens

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.tokens.TokensFeatureToggles

internal class DefaultTokensFeatureToggles(
    @Suppress("UnusedPrivateMember") private val featureTogglesManager: FeatureTogglesManager,
) : TokensFeatureToggles