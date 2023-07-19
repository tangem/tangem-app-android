package com.tangem.tap.features.shop.data

import com.tangem.datasource.api.tangemTech.models.SalesResponse
import com.tangem.tap.features.shop.domain.models.Notification
import com.tangem.tap.features.shop.domain.models.ProductState
import com.tangem.tap.features.shop.domain.models.ProductType
import com.tangem.tap.features.shop.domain.models.SalesProduct
import com.tangem.utils.converter.Converter

internal class SalesProductConverter : Converter<SalesResponse, List<SalesProduct>> {

    override fun convert(value: SalesResponse): List<SalesProduct> {
        return value.sales.map { sales ->
            val productState = when (sales.state) {
                "order" -> ProductState.ORDER
                "pre-order" -> ProductState.PRE_ORDER
                "sold-out" -> ProductState.SOLD_OUT
                else -> ProductState.SOLD_OUT
            }
            val productType = when (sales.product.code) {
                "pack2" -> ProductType.WALLET_2_CARDS
                "pack3" -> ProductType.WALLET_3_CARDS
                else -> ProductType.WALLET_3_CARDS
            }
            SalesProduct(
                id = sales.id,
                productType = productType,
                state = productState,
                name = sales.product.name,
                notification = sales.notification?.let { notification ->
                    Notification(
                        type = notification.type,
                        title = notification.title,
                        description = notification.description,
                    )
                },
            )
        }
    }
}
