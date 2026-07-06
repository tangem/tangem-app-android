package com.tangem.features.tangempay

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal class DefaultTangemPayFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : TangemPayFeatureToggles {
    override val isRedesignEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_15368_VISA_PAY_REDESIGN) &&
            featureTogglesManager.isFeatureEnabled(FeatureToggles.APP_REDESIGN_ENABLED)

    override val isCloseCardEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_15364_VISA_PAY_CARD_CLOSE)

    override val isRemoveAccountEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_15741_VISA_PAY_REMOVE_ACCOUNT)

    override val isMultipleCardsEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_15235_VISA_MULTIPLE_CARDS)

    override val isTiersPlusPlanEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_16041_VISA_TIERS_PLUS_PLAN)
}