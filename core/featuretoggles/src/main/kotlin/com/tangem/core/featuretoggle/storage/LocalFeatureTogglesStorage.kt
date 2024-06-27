package com.tangem.core.featuretoggle.storage

import com.tangem.core.featuretoggle.storage.LocalFeatureTogglesStorage.Companion.LOCAL_CONFIG_PATH
import com.tangem.datasource.asset.loader.AssetLoader
import kotlin.properties.Delegates

/**
 * Storage implementation for storing local feature toggles.
 * Feature toggles are declared in file [LOCAL_CONFIG_PATH].
 *
 * @property assetLoader asset loader
 *
[REDACTED_AUTHOR]
 */
internal class LocalFeatureTogglesStorage(
    private val assetLoader: AssetLoader,
) : FeatureTogglesStorage {

    override var featureToggles: List<FeatureToggle> by Delegates.notNull()
        private set

    override suspend fun init() {
        featureToggles = assetLoader.loadList<FeatureToggle>(LOCAL_CONFIG_PATH)
    }

    private companion object {
        const val LOCAL_CONFIG_PATH: String = "configs/feature_toggles_config"
    }
}