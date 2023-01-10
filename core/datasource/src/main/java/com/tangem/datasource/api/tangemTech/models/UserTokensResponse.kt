package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json

data class UserTokensResponse(
    @Json(name = "version") val version: Int = 0,
    @Json(name = "group") val group: String? = null,
    @Json(name = "sort") val sort: String? = null,
    @Json(name = "tokens") val tokens: List<Token> = emptyList(),
) {

    data class Token(
        @Json(name = "id") val id: String? = null,
        @Json(name = "networkId") val networkId: String,
        @Json(name = "derivationPath") val derivationPath: String? = null,
        @Json(name = "name") val name: String,
        @Json(name = "symbol") val symbol: String,
        @Json(name = "decimals") val decimals: Int,
        @Json(name = "contractAddress") val contractAddress: String?,
    )
}
