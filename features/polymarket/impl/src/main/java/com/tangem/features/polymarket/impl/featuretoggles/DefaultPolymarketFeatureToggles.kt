package com.tangem.features.polymarket.impl.featuretoggles

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import javax.inject.Inject

internal class DefaultPolymarketFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : PolymarketFeatureToggles {
    override val isPolymarketPhaseTwoEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1700_POLYMARKET_PHASE_TWO_ENABLED)

    override val isPolymarketFastOrdersEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1881_POLYMARKET_FAST_ORDERS_ENABLED)

    override val isPolymarketCryptoUpDownEnabled: Boolean
        get() = isPolymarketPhaseTwoEnabled &&
            isPolymarketFastOrdersEnabled &&
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1882_POLYMARKET_CRYPTO_UP_DOWN_ENABLED)
}