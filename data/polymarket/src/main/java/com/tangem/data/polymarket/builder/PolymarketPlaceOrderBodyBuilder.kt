package com.tangem.data.polymarket.builder

import com.tangem.grow.datasource.polymarket.clob.models.PolymarketPlaceOrderRequest
import com.tangem.domain.polymarket.model.PredictionSignedOrder
import kotlinx.serialization.json.Json

internal object PolymarketPlaceOrderBodyBuilder {

    fun build(signedOrder: PredictionSignedOrder, apiKey: String): String {
        val order = signedOrder.order

        val request = PolymarketPlaceOrderRequest(
            order = PolymarketPlaceOrderRequest.Order(
                salt = order.salt.toString(),
                maker = order.maker,
                signer = order.signer,
                tokenId = order.tokenId.toString(),
                makerAmount = order.makerAmount.toString(),
                takerAmount = order.takerAmount.toString(),
                side = order.side.name,
                signatureType = order.signatureType,
                timestamp = order.timestampMillis.toString(),
                metadata = order.metadata,
                builder = order.builder,
                signature = signedOrder.signature,
            ),
            owner = apiKey,
            orderType = PolymarketPlaceOrderRequest.OrderType.FILL_AND_KILL,
        )

        return Json.encodeToString(PolymarketPlaceOrderRequest.serializer(), request)
    }
}