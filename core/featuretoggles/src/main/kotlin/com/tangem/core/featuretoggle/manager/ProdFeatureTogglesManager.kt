package com.tangem.core.featuretoggle.manager

import androidx.annotation.VisibleForTesting
import com.tangem.core.featuretoggle.storage.FeatureTogglesStorage
import com.tangem.core.featuretoggle.utils.associateToggles
import com.tangem.core.featuretoggle.version.VersionProvider
import kotlin.properties.Delegates

/**
 * Feature toggles manager implementation in PROD build
 *
 * @property localFeatureTogglesStorage local feature toggles storage
 * @property versionProvider            application version provider
 */
internal class ProdFeatureTogglesManager(
    private val localFeatureTogglesStorage: FeatureTogglesStorage,
    private val versionProvider: VersionProvider,
) : FeatureTogglesManager {

    private var featureToggles: Map<String, Boolean> by Delegates.notNull()

    override suspend fun init() {
        localFeatureTogglesStorage.init()
        featureToggles = localFeatureTogglesStorage.featureToggles
            .associateToggles(currentVersion = versionProvider.get() ?: "")
    }

    override fun isFeatureEnabled(name: String): Boolean = featureToggles[name] ?: false

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun getProdFeatureToggles() = featureToggles

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun setProdFeatureToggles(map: Map<String, Boolean>) {
        featureToggles = map
    }
}
