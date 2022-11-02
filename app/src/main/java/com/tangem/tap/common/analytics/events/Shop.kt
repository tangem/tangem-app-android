package com.tangem.tap.common.analytics.events

import com.shopify.buy3.Storefront
import com.tangem.common.Converter
import com.tangem.tap.common.extensions.filterNotNull
import com.tangem.tap.common.shop.data.ProductType

/**
[REDACTED_AUTHOR]
 */
sealed class Shop(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Shop", event, params) {

    class ScreenOpened : IntroductionProcess("Shop Screen Opened")

    class Purchased(params: Map<String, String>) : Shop(
        event = "Purchased",
        params = params,
    )

    class Redirected(partnerName: String) : Shop(
        event = "Redirected",
        params = mapOf("Partner" to partnerName),
    )
}

class OrderToParamsConverter : Converter<Pair<Storefront.Order, ProductType>, Map<String, String>> {
    override fun convert(value: Pair<Storefront.Order, ProductType>): Map<String, String> {
        val order = value.first
        val productType = value.second

        val code = (order.discountApplications.edges.firstOrNull()?.node as? Storefront.DiscountCodeApplication)?.code
        val sku = order.lineItems.edges.firstOrNull()?.node?.variant?.sku ?: productType.sku

        val count = when (productType) {
            ProductType.WALLET_2_CARDS -> "2"
            ProductType.WALLET_3_CARDS -> "3"
        }
        val amount = "${order.totalPriceV2.amount} ${order.totalPriceV2.currencyCode.name}"

        return mapOf(
            "Coupon Code" to code,
            "SKU" to sku,
            "Count" to count,
            "Amount" to amount,
        ).filterNotNull()
    }
}
