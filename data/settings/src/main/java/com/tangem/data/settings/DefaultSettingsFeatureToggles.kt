package com.tangem.data.settings

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.settings.SettingsFeatureToggles
import javax.inject.Inject

internal class DefaultSettingsFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : SettingsFeatureToggles {

    override val isGeoCountryCacheFallbackEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            FeatureToggles.AND_17136_GEO_COUNTRY_CACHE_FALLBACK_ENABLED,
        )
}