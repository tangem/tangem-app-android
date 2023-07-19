package com.tangem.tap.features.shop.toggles

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager

internal class DefaultShopifyFeatureToggleManager(
    private val featureTogglesManager: FeatureTogglesManager,
) : ShopifyFeatureToggleManager {

    override val isDynamicSalesProductsEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("SHOPIFY_DYNAMIC_ENABLED")
}
