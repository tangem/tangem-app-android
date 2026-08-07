package com.tangem.data.pay.util

import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.model.PlasticCardOrder
import com.tangem.spend.datasource.pay.models.request.OrderRequest

internal object PlasticIssueOrderRequestConverter {

    fun convert(
        customerWalletAddress: String,
        specificationName: String,
        order: PlasticCardOrder,
        idempotencyKey: String,
    ): OrderRequest = OrderRequest(
        data = OrderRequest.Data(
            customerWalletAddress = customerWalletAddress,
            specificationName = specificationName,
            type = OrderType.CARD_ISSUE_PLASTIC_RAIN.wireValue,
            embossName = order.embossName,
            shippingAddress = OrderRequest.ShippingAddress(
                firstName = order.shippingAddress.firstName,
                lastName = order.shippingAddress.lastName,
                line1 = order.shippingAddress.line1,
                line2 = order.shippingAddress.line2,
                city = order.shippingAddress.city,
                region = order.shippingAddress.region,
                postalCode = order.shippingAddress.postalCode,
                phone = order.shippingAddress.phone,
            ),
        ),
        idempotencyKey = idempotencyKey,
    )
}