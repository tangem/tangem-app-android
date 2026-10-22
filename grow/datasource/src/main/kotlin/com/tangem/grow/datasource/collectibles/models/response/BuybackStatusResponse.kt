package com.tangem.grow.datasource.collectibles.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response for POST /api/v1/buyback/buyback-tx-sent and GET /api/v1/buyback/status */
@Serializable
data class BuybackStatusResponse(
    @SerialName("buybackId")
    val buybackId: String,
    @SerialName("status")
    val status: BuybackStatusDTO = BuybackStatusDTO.UNKNOWN,
) {

    @Serializable
    enum class BuybackStatusDTO {
        @SerialName("created")
        CREATED,

        @SerialName("waiting-tx-hash")
        WAITING_TX_HASH,

        @SerialName("buyback-tx-sent")
        BUYBACK_TX_SENT,

        @SerialName("confirming")
        CONFIRMING,

        @SerialName("confirmed")
        CONFIRMED,

        @SerialName("expired")
        EXPIRED,

        /** Not a wire value: a status this build does not know is coerced to this default by `NetworkJsonFactory`. */
        UNKNOWN,
    }
}