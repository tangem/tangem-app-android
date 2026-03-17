package com.tangem.tap.common.analytics

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import javax.inject.Inject

class CustomerIoFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) {

    val isFeatureEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(toggle = FeatureToggles.CUSTOMER_IO_ENABLED)
}