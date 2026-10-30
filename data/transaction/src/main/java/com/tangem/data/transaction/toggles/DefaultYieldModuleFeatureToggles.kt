package com.tangem.data.transaction.toggles

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.transaction.YieldModuleFeatureToggles
import com.tangem.utils.annotations.RemoveWithToggle

@RemoveWithToggle("AND_18834_YIELD_MODULE_V3_ENABLED")
internal class DefaultYieldModuleFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : YieldModuleFeatureToggles {

    override val isYieldModuleV3Enabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_18834_YIELD_MODULE_V3_ENABLED)
}