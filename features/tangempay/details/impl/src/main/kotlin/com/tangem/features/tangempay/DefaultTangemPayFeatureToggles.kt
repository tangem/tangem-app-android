package com.tangem.features.tangempay

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.utils.annotations.RemoveWithToggle

internal class DefaultTangemPayFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : TangemPayFeatureToggles {
    override val isGoogleWalletPushProvisioningEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1062_GOOGLE_WALLET_PUSH_PROVISIONING_ENABLED)

    @RemoveWithToggle(
        toggleName = "TWI_1062_GOOGLE_WALLET_PUSH_PROVISIONING_ENABLED",
        description = "debug switch, drop TWI_1062_GOOGLE_WALLET_BUTTON_ALWAYS_SHOWN with the interface property",
    )
    override val isGoogleWalletButtonAlwaysShown: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1062_GOOGLE_WALLET_BUTTON_ALWAYS_SHOWN)

    override val isOrderFrameworkEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_15342_ORDER_FRAMEWORK_ENABLED)

    override val isCompactEmptyHistoryEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_19706_PAY_COMPACT_EMPTY_HISTORY_ENABLED)

    override val isCardActionsEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_19693_PAY_CARD_ACTIONS_ENABLED)
}