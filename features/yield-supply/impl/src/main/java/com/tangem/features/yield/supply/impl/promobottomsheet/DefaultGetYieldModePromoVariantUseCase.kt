package com.tangem.features.yield.supply.impl.promobottomsheet

import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.features.yield.supply.api.YieldSupplyFeatureToggles
import com.tangem.features.yield.supply.api.promobottomsheet.GetYieldModePromoVariantUseCase
import com.tangem.features.yield.supply.api.promobottomsheet.YieldModePromoVariant
import javax.inject.Inject

internal class DefaultGetYieldModePromoVariantUseCase @Inject constructor(
    private val featureToggles: YieldSupplyFeatureToggles,
    private val abTestsManager: ABTestsManager,
) : GetYieldModePromoVariantUseCase {

    override suspend fun invoke(): YieldModePromoVariant {
        if (!featureToggles.isYieldModePromoEnabled) return YieldModePromoVariant.CONTROL

        // An unassigned user holds the flag's empty `off` default, and reading even that tracks an exposure
        abTestsManager.peekValue(key = AMPLITUDE_ID) ?: return YieldModePromoVariant.CONTROL

        return when (abTestsManager.getValue(key = AMPLITUDE_ID, defaultValue = CONTROL_VALUE)) {
            USER_MAX_APY_VALUE -> YieldModePromoVariant.USER_MAX_APY
            GLOBAL_MAX_APY_VALUE -> YieldModePromoVariant.GLOBAL_MAX_APY
            else -> YieldModePromoVariant.CONTROL
        }
    }

    private companion object {
        const val AMPLITUDE_ID = "twi_1762_yield_mode_promo"
        const val CONTROL_VALUE = "control"
        const val USER_MAX_APY_VALUE = "user_max_apy"
        const val GLOBAL_MAX_APY_VALUE = "global_max_apy"
    }
}