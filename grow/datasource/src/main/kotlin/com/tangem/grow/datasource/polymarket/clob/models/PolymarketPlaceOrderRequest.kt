package com.tangem.grow.datasource.polymarket.clob.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PolymarketPlaceOrderRequest(
    @SerialName("order") val order: Order,
    @SerialName("owner") val owner: String,
    @SerialName("orderType") val orderType: OrderType,
    @SerialName("postOnly") val isPostOnly: Boolean,
    @SerialName("deferExec") val isDeferExec: Boolean,
) {

    @Serializable
    data class Order(
        @SerialName("salt") val salt: Long,
        @SerialName("maker") val maker: String,
        @SerialName("signer") val signer: String,
        @SerialName("tokenId") val tokenId: String,
        @SerialName("makerAmount") val makerAmount: String,
        @SerialName("takerAmount") val takerAmount: String,
        @SerialName("side") val side: String,
        @SerialName("signatureType") val signatureType: Int,
        @SerialName("timestamp") val timestamp: String,
        @SerialName("expiration") val expiration: String,
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