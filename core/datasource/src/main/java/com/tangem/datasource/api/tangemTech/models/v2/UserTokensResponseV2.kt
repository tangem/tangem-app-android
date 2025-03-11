package com.tangem.datasource.api.tangemTech.models.v2

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse

@JsonClass(generateAdapter = true)
data class UserTokensResponseV2(
    @Json(name = "accounts")
    val accounts: List<TokensAccount>,
) {

    @JsonClass(generateAdapter = true)
    data class TokensAccount(
        @Json(name = "id")
        val id: Int,
        @Json(name = "title")
        val title: String,
        @Json(name = "tokens")
        val tokens: List<UserTokensResponse.Token>? = null,
        @Json(name = "tokensCount")
        val tokensCount: Int? = null,
        @Json(name = "archived")
        val isArchived: Boolean,
    )
}