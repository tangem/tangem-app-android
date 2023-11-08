package com.tangem.features.send.impl.featuretoggles

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.features.send.api.featuretoggles.SendFeatureToggles

/**
 * Default implementation of Send feature toggles
 *
 * @property featureTogglesManager manager for getting information about the availability of feature toggles
 */
internal class DefaultSendFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : SendFeatureToggles {
    override val isRedesignedSendEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "REDESIGNED_SEND_SCREEN_ENABLED")
}