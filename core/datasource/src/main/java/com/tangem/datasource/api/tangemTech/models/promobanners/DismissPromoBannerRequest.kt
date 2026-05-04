package com.tangem.datasource.api.tangemTech.models.promobanners

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DismissPromoBannerRequest(
    @Json(name = "walletId") val walletId: String,
    @Json(name = "status") val status: BannerDisplayStatus,
) {

    @JsonClass(generateAdapter = false)
    enum class BannerDisplayStatus {
        @Json(name = "active") ACTIVE,

        @Json(name = "dismissed") DISMISSED,
    }
}