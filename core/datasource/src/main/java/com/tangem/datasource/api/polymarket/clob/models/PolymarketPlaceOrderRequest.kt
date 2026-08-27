package com.tangem.datasource.api.polymarket.clob.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PolymarketPlaceOrderRequest(
    @SerialName("order") val order: Order,
    @SerialName("owner") val owner: String,
    @SerialName("orderType") val orderType: OrderType,
) {

    @Serializable
    data class Order(
        @SerialName("salt") val salt: String,
        @SerialName("maker") val maker: String,
        @SerialName("signer") val signer: String,
        @SerialName("tokenId") val tokenId: String,
        @SerialName("makerAmount") val makerAmount: String,
        @SerialName("takerAmount") val takerAmount: String,
        @SerialName("side") val side: String,
        @SerialName("signatureType") val signatureType: Int,
        @SerialName("timestamp") val timestamp: String,
        @SerialName("metadata") val metadata: String,
        @SerialName("builder") val builder: String,
        @SerialName("signature") val signature: String,
    )

    @Serializable
    enum class OrderType {
        @SerialName("FAK")
        FILL_AND_KILL,

        @SerialName("FOK")
        FILL_OR_KILL,

        @SerialName("GTC")
        GOOD_TILL_CANCELLED,

        @SerialName("GTD")
        GOOD_TILL_DATE,
    }
}