package com.tangem.core.featuretoggle.manager

import com.tangem.core.featuretoggle.comparator.VersionContract
import com.tangem.core.featuretoggle.storage.FeatureTogglesStorage
import com.tangem.core.featuretoggle.utils.associate
import kotlin.properties.Delegates

/**
 * Manager implementation for getting information about the availability of feature toggles
 * in PROD build
 *
 * @property localFeatureTogglesStorage storage of local feature toggles
 * @property versionContract            contract that returns the availability of feature toggle version
 */
internal class ProdFeatureTogglesManager(
    private val localFeatureTogglesStorage: FeatureTogglesStorage,
    private val versionContract: VersionContract,
) : FeatureTogglesManager {

    private var featureToggles: Map<String, Boolean> by Delegates.notNull()

    override suspend fun init() {
        localFeatureTogglesStorage.init()
        featureToggles = localFeatureTogglesStorage.featureToggles.associate(versionContract)
    }

    override fun isFeatureEnabled(toggle: IFeatureToggle): Boolean = featureToggles.any { it.key == toggle.name }
}
