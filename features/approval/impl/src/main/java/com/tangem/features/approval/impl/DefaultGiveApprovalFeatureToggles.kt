package com.tangem.features.approval.impl

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.approval.api.GiveApprovalFeatureToggles

internal class DefaultGiveApprovalFeatureToggles(
    private val featureToggles: FeatureTogglesManager,
) : GiveApprovalFeatureToggles {

    // Remove GiveTxPermissionBottomSheet and all dependencies with this toggle
    override val isGaslessApprovalEnabled: Boolean
        get() = featureToggles.isFeatureEnabled("GASLESS_APPROVAL_ENABLED")
}