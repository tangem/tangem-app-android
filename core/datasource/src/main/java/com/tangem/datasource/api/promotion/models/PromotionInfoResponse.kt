package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json

/**
[REDACTED_AUTHOR]
 */
data class PromotionInfoResponse(
    @Json(name = "status") val status: Status?,
    @Json(name = "awardForNewCard") val awardForNewCard: Float?,
    @Json(name = "awardForOldCard") val awardForOldCard: Float?,
    @Json(name = "awardPaymentToken") val awardPaymentToken: TokenInfo?,
    @Json(name = "error") override val error: Error? = null,
) : AbstractPromotionResponse() {

    enum class Status(val value: String) {
        @Json(name = "pending")
        PENDING("pending"),

        @Json(name = "active")
        ACTIVE("active"),

        @Json(name = "finished")
        FINISHED("finished"),
    }

    data class TokenInfo(
        @Json(name = "id") val id: String,
        @Json(name = "name") val name: String,
        @Json(name = "symbol") val symbol: String,
        @Json(name = "active") val active: Boolean,
        @Json(name = "networks") val networks: List<Network>,
    ) {
        data class Network(
            @Json(name = "networkId") val networkId: String,
            @Json(name = "exchangeable") val exchangeable: Boolean,
            @Json(name = "contractAddress") val contractAddress: String,
            @Json(name = "decimalCount") val decimalCount: Int,
        )
    }
}