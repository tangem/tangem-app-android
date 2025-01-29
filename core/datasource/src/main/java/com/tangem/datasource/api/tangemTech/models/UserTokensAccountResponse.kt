package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserTokensAccountResponse(
    @Json(name = "accountId")
    val id: Int,
    @Json(name = "accountTitle")
    val title: String,
    @Json(name = "archived")
    val isArchived: Boolean,
    @Json(name = "tokensCount")
    val tokensCount: Int? = null,
    @Json(name = "tokens")
    val tokens: List<UserTokensResponse.Token>? = null,
)