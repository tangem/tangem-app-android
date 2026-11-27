package com.tangem.features.earn.impl.flow

import com.tangem.features.earn.api.EarnEntryComponent
import com.tangem.features.earn.flow.api.EarnFlowComponent
import com.tangem.features.earn.flow.api.EarnFlowProduct
import com.tangem.features.earn.impl.overview.EarnProductsRegistry
import com.tangem.features.earn.product.api.navigation.EarnFlowRoute
import com.tangem.features.earn.product.api.product.EarnProduct
import javax.inject.Inject

internal class EarnFlowParamsFactory @Inject constructor(
    private val registry: EarnProductsRegistry,
) {

    fun create(
        route: EarnFlowRoute,
        entry: EarnEntryComponent.Params,
        callback: EarnFlowComponent.ModelCallback,
    ): EarnFlowComponent.Params? {
        val product = registry.find(route.productId) ?: return null

        return EarnFlowComponent.Params(
            product = EarnProduct.Params(userWalletId = entry.userWalletId, currency = entry.cryptoCurrency),
            flowProduct = EarnFlowProduct(
                productId = product.id,
                title = product.title,
                provider = product.provider,
                analytics = product.analytics,
                howItWorks = product.howItWorks,
                legal = product.legal,
            ),
            action = route.action,
            source = entry.source,
            callback = callback,
            opener = route.opener,
        )
    }
}