package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.common.extensions.calculateHashCode

@JsonClass(generateAdapter = true)
data class UserTokensResponse(
    @Json(name = "version") val version: Int = 0,
    @Json(name = "group") val group: GroupType,
    @Json(name = "sort") val sort: SortType,
    @Json(name = "notifyStatus") val notifyStatus: Boolean? = null,
    @Json(name = "tokens") val tokens: List<Token> = emptyList(),
) {

    @JsonClass(generateAdapter = true)
    data class Token(
        @Json(name = "id") val id: String? = null,
        @Json(name = "networkId") val networkId: String,
        @Json(name = "derivationPath") val derivationPath: String? = null,
        @Json(name = "name") val name: String,
        @Json(name = "symbol") val symbol: String,
        @Json(name = "decimals") val decimals: Int,
        @Json(name = "contractAddress") val contractAddress: String?,
        @Json(name = "addresses") val addresses: List<String>? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            val otherToken = other as? Token ?: return false

            // use only this fields is enough and correct to compare, don't add id or smth
            // to avoid duplicates for tokens on main screen
            return otherToken.contractAddress == this.contractAddress &&
                otherToken.networkId == this.networkId &&
                otherToken.derivationPath == this.derivationPath &&
                otherToken.decimals == this.decimals
        }

        override fun hashCode(): Int = calculateHashCode(
            contractAddress.hashCode(),
            networkId.hashCode(),
            derivationPath.hashCode(),
            decimals.hashCode(),
        )
    }

    @JsonClass(generateAdapter = false)
    enum class GroupType {
        @Json(name = "none")
        NONE,

        @Json(name = "token")
        TOKEN,

        @Json(name = "network")
        NETWORK,
    }

    @JsonClass(generateAdapter = false)
    enum class SortType {
        @Json(name = "balance")
        BALANCE,

        @Json(name = "manual")
        MANUAL,

        @Json(name = "marketcap")
        MARKETCAP,
    }
}