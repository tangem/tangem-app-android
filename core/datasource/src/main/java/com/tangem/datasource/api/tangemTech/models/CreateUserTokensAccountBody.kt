package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateUserTokensAccountBody(
    @Json(name = "accountId")
    val id: Int,
    @Json(name = "accountTitle")
    val title: String,
)