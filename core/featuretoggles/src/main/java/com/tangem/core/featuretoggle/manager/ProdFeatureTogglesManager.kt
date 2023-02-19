package com.tangem.core.featuretoggle.manager

import android.content.Context
import com.tangem.core.featuretoggle.FeatureToggle
import com.tangem.core.featuretoggle.storage.FeatureTogglesStorage
import com.tangem.core.featuretoggle.utils.associateToggles
import com.tangem.core.featuretoggle.utils.getVersion
import kotlin.properties.Delegates

/**
 * Feature toggles manager implementation in PROD build
 *
 * @property localFeatureTogglesStorage local feature toggles storage
 * @property context                    context
 */
internal class ProdFeatureTogglesManager(
    private val localFeatureTogglesStorage: FeatureTogglesStorage,
    private val context: Context,
) : FeatureTogglesManager {

    private var featureToggles: Map<String, Boolean> by Delegates.notNull()

    override suspend fun init() {
        localFeatureTogglesStorage.init()
        featureToggles = localFeatureTogglesStorage.featureToggles
            .associateToggles(currentVersion = context.getVersion() ?: "")
    }

    override fun isFeatureEnabled(toggle: FeatureToggle): Boolean = featureToggles.any { it.key == toggle.name }
}
