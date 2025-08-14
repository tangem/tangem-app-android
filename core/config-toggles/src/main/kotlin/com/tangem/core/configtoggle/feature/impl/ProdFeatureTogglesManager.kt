package com.tangem.core.configtoggle.feature.impl

import androidx.annotation.VisibleForTesting
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.configtoggle.storage.TogglesStorage
import com.tangem.core.configtoggle.utils.associateToggles
import com.tangem.core.configtoggle.version.VersionProvider

/**
 * Feature toggles manager implementation in PROD build
 *
 * @property localTogglesStorage local feature toggles storage
 * @property versionProvider            application version provider
 */
internal class ProdFeatureTogglesManager(
    private val localTogglesStorage: TogglesStorage,
    private val versionProvider: VersionProvider,
) : FeatureTogglesManager {

    private var featureToggles: Map<String, Boolean>? = null

    override suspend fun init() {
        if (featureToggles != null) {
            return // Already initialized
        }

        localTogglesStorage.populate(FeatureTogglesConstants.LOCAL_CONFIG_PATH)
        featureToggles = localTogglesStorage.toggles
            .associateToggles(currentVersion = versionProvider.get() ?: "")
    }

    override fun isFeatureEnabled(name: String): Boolean = featureToggles!![name] ?: false

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun getProdFeatureToggles() = featureToggles!!

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun setProdFeatureToggles(map: Map<String, Boolean>) {
        featureToggles = map
    }
}