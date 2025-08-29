package com.tangem.core.configtoggle.feature.impl

import androidx.annotation.VisibleForTesting
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.configtoggle.utils.defineTogglesAvailability
import com.tangem.core.configtoggle.version.VersionProvider

/**
 * Feature toggles manager implementation in PROD build
 *
 * @property versionProvider application version provider
 */
internal class ProdFeatureTogglesManager(
    private val versionProvider: VersionProvider,
) : FeatureTogglesManager {

    private val featureToggles: Map<String, Boolean> = getFileFeatureToggles()

    override fun isFeatureEnabled(name: String): Boolean = featureToggles[name] == true

    private fun getFileFeatureToggles(): Map<String, Boolean> {
        val appVersion = versionProvider.get()

        return FeatureToggles.values.defineTogglesAvailability(appVersion = appVersion)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun getProdFeatureToggles() = featureToggles
}