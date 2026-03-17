package com.tangem.features.promobanners.impl.toggles

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.promobanners.api.NewPromoBannersFeatureToggles
import javax.inject.Inject

internal class DefaultNewPromoBannersFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : NewPromoBannersFeatureToggles {

    override val isNewPromoBannersEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(toggle = FeatureToggles.NEW_PROMO_BANNERS_ENABLED)
}