package com.tangem.data.onramp

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.onramp.repositories.OnrampFeatureToggles

internal class DefaultOnrampFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : OnrampFeatureToggles {

    override val isThemedPaymentMethodImagesEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.AND_16359_ONRAMP_THEMED_PAYMENT_METHOD_IMAGES,
        )
}