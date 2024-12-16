package com.tangem.tap.features.home.featuretoggles

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import javax.inject.Inject

class HomeFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) {

    val isMigrateUserCountryCodeEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "MIGRATE_USER_COUNTRY_CODE_ENABLED")
}