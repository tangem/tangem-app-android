package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MarkUserWalletWasCreatedBody(
    @Json(name = "user_wallet_id") val userWalletId: String,
)