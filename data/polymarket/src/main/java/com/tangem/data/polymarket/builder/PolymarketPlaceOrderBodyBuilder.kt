package com.tangem.data.polymarket.builder

import com.tangem.grow.datasource.polymarket.clob.models.PolymarketPlaceOrderRequest
import com.tangem.domain.polymarket.model.PredictionSignedOrder
import kotlinx.serialization.json.Json

internal object PolymarketPlaceOrderBodyBuilder {

    private const val NO_EXPIRATION = "0"

    fun build(signedOrder: PredictionSignedOrder, apiKey: String): String {
        val order = signedOrder.order

        val request = PolymarketPlaceOrderRequest(
            order = PolymarketPlaceOrderRequest.Order(
                salt = order.salt,
                maker = order.maker.lowercase(),
                signer = order.signer.lowercase(),
                tokenId = order.tokenId.toString(),
                makerAmount = order.makerAmount.toString(),
                takerAmount = order.takerAmount.toString(),
                side = order.side.name,
                signatureType = order.signatureType,
                timestamp = order.timestampMillis.toString(),
                expiration = NO_EXPIRATION,
                metadata = order.metadata,
                builder = order.builder,
                signature = signedOrder.signature,
            ),
            owner = apiKey,
            orderType = PolymarketPlaceOrderRequest.OrderType.FILL_AND_KILL,
            isPostOnly = false,
            isDeferExec = false,
        )

        return Json.encodeToString(PolymarketPlaceOrderRequest.serializer(), request)
    }
}