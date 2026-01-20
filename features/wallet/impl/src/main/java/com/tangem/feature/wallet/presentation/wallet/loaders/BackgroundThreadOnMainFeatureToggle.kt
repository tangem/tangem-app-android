package com.tangem.feature.wallet.presentation.wallet.loaders

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import javax.inject.Inject

internal class BackgroundThreadOnMainFeatureToggle @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) {
    val isEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("BACKGROUND_THREAD_ON_MAIN_ENABLED")
}