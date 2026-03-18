package com.tangem.datasource.api.tangemTech.models.promobanners

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DismissPromoBannerRequest(
    @Json(name = "walletId") val walletId: String,
    @Json(name = "isDismissed") val isDismissed: Boolean,
)