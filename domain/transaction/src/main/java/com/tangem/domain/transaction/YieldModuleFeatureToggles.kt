package com.tangem.domain.transaction

import com.tangem.utils.annotations.RemoveWithToggle

@RemoveWithToggle("AND_18834_YIELD_MODULE_V3_ENABLED")
interface YieldModuleFeatureToggles {

    val isYieldModuleV3Enabled: Boolean
}