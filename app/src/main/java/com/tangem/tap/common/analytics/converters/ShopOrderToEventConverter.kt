package com.tangem.tap.common.analytics.converters

import com.shopify.buy3.Storefront
import com.tangem.common.Converter
import com.tangem.tap.common.analytics.events.Shop
import com.tangem.tap.common.shop.data.ProductType

/**
 * Created by Anton Zhilenkov on 02.11.2022.
 */
class ShopOrderToEventConverter : Converter<Pair<Storefront.Order, ProductType>, Shop.Purchased> {

    override fun convert(value: Pair<Storefront.Order, ProductType>): Shop.Purchased {
        val order = value.first
        val productType = value.second

        val sku = order.lineItems.edges.firstOrNull()?.node?.variant?.sku ?: productType.sku
        val count = when (productType) {
            ProductType.WALLET_2_CARDS -> "2"
            ProductType.WALLET_3_CARDS -> "3"
        }
        val amount = "${order.totalPriceV2.amount} ${order.totalPriceV2.currencyCode.name}"
        val code = (order.discountApplications.edges.firstOrNull()?.node as? Storefront.DiscountCodeApplication)?.code

        return Shop.Purchased(sku, count, amount, code)
    }
}
