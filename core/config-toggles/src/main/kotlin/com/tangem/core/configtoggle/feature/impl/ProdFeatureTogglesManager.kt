package com.tangem.core.configtoggle.feature.impl

import androidx.annotation.VisibleForTesting
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.configtoggle.feature.provider.FeatureTogglesProvider
import com.tangem.core.configtoggle.utils.defineTogglesAvailability
import com.tangem.core.configtoggle.version.VersionProvider

/**
 * Feature toggles manager implementation in PROD build
 *
 * @property versionProvider         application version provider
 * @property featureTogglesProvider  provider for feature toggle entries
 */
internal class ProdFeatureTogglesManager(
    private val versionProvider: VersionProvider,
    private val featureTogglesProvider: FeatureTogglesProvider,
) : FeatureTogglesManager {

    private val featureToggles: Map<String, Boolean> = getFileFeatureToggles()

    override fun isFeatureEnabled(toggle: FeatureToggles): Boolean = featureToggles[toggle.rawName] == true

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun isFeatureEnabledByName(name: String): Boolean = featureToggles[name] == true

    private fun getFileFeatureToggles(): Map<String, Boolean> {
        val appVersion = versionProvider.get()

        return featureTogglesProvider.getToggles()
            .defineTogglesAvailability(appVersion = appVersion)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun getProdFeatureToggles() = featureToggles
}