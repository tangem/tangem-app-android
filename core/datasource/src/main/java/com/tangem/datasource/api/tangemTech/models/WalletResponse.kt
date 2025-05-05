package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WalletResponse(
    @Json(name = "notifyStatus") val notifyStatus: Boolean,
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String? = null,
)