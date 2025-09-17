package com.tangem.datasource.api.tangemTech.models.account

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse

@JsonClass(generateAdapter = true)
data class WalletAccountDTO(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String?,
    @Json(name = "derivation") val derivationIndex: Int,
    @Json(name = "icon") val icon: String,
    @Json(name = "iconColor") val iconColor: String,
    @Json(name = "tokens") val tokens: List<UserTokensResponse.Token>? = null,
    @Json(name = "totalTokens") val totalTokens: Int? = null,
    @Json(name = "totalNetworks") val totalNetworks: Int? = null,
)