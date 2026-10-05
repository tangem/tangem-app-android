package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PromotionRegistrationsResponse(
    @Json(name = "id") val id: Int,
    @Json(name = "results") val results: List<ResultDto>,
) {

    /** [payout] is `null` when the campaign has no `payoutNeeded`, even for an enrolled wallet. */
    @JsonClass(generateAdapter = true)
    data class ResultDto(
        @Json(name = "walletId") val walletId: String,
        @Json(name = "status") val status: Status,
        @Json(name = "payout") val payout: PayoutDto?,
    )

    @JsonClass(generateAdapter = true)
    data class PayoutDto(
        @Json(name = "userAddress") val userAddress: String,
        @Json(name = "tokenAddress") val tokenAddress: String,
        @Json(name = "networkId") val networkId: String,
    )

    /** Values unknown to this app version parse as `UNKNOWN`, see `addPromotionEnumFallbackAdapters`. */
    @JsonClass(generateAdapter = false)
    enum class Status {
        @Json(name = "enrolled")
        ENROLLED,

        @Json(name = "already_enrolled")
        ALREADY_ENROLLED,

        @Json(name = "ineligible")
        INELIGIBLE,

        UNKNOWN,
    }
}