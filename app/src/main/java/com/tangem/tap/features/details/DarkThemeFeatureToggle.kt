package com.tangem.tap.features.details

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager

class DarkThemeFeatureToggle(
    private val featureTogglesManager: FeatureTogglesManager,
) {
    val isDarkThemeEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "DARK_THEME_ENABLED")
}
