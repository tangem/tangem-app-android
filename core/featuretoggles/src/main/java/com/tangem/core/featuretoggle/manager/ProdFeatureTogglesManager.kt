package com.tangem.core.featuretoggle.manager

import com.tangem.core.featuretoggle.FeatureToggle
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

    override fun isFeatureEnabled(toggle: FeatureToggle): Boolean = featureToggles.any { it.key == toggle.name }
}
