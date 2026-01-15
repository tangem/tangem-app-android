package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BindWalletsByReferralCodeBody(
    @Json(name = "walletIds") val walletIds: List<String>,
    @Json(name = "referralCode") val refcode: String,
    @Json(name = "utmCampaign") val campaign: String? = null,
)