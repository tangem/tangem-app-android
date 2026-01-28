package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AssociateAppWithWalletsErrorResponse(
    @Json(name = "missingWalletIds") val missingWalletIds: List<String>,
)