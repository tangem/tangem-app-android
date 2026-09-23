package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AssociateApplicationIdWithWalletsBody(
    @Json(name = "walletIds") val walletIds: List<String>,
)