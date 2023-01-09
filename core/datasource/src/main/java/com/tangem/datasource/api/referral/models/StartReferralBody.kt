package com.tangem.datasource.api.referral.models

import com.squareup.moshi.Json

/** Body for make user referral */
data class StartReferralBody(
    @Json(name = "walletId") val walletId: String,
    @Json(name = "networkId") val networkId: String,
    @Json(name = "tokenId") val tokenId: String,
    @Json(name = "address") val address: String,
)
