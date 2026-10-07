package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Body of `POST /api/referral/v1/promotion/{id}/registrations`. `walletId`s must be unique and the list non-empty.
 * Payout fields are required only when the campaign has `payoutNeeded`; `null` fields are omitted from the JSON.
 */
@JsonClass(generateAdapter = true)
data class PromotionRegistrationsBody(
    @Json(name = "enrollments") val enrollments: List<EnrollmentDto>,
) {

    /** [isMain] marks the wallet that initiated the registration; the backend reads it only for `linked_wallet`. */
    @JsonClass(generateAdapter = true)
    data class EnrollmentDto(
        @Json(name = "walletId") val walletId: String,
        @Json(name = "walletType") val walletType: PromotionWalletType,
        @Json(name = "batchId") val batchId: String?,
        @Json(name = "userAddress") val userAddress: String?,
        @Json(name = "tokenAddress") val tokenAddress: String?,
        @Json(name = "networkId") val networkId: String?,
        @Json(name = "tokenId") val tokenId: String?,
        @Json(name = "main") val isMain: Boolean,
    )
}