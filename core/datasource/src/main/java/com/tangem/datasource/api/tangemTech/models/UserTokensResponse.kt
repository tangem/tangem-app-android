package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.tangem.common.extensions.calculateHashCode

data class UserTokensResponse(
    @Json(name = "version") val version: Int = 0,
    @Json(name = "group") val group: GroupType,
    @Json(name = "sort") val sort: SortType,
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
    ) {
        override fun equals(other: Any?): Boolean {
            val otherToken = other as? Token ?: return false

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

    enum class GroupType {
        @Json(name = "none")
        NONE,

        @Json(name = "token")
        TOKEN,

        @Json(name = "network")
        NETWORK,
    }

    enum class SortType {
        @Json(name = "balance")
        BALANCE,

        @Json(name = "manual")
        MANUAL,

        @Json(name = "marketcap")
        MARKETCAP,
    }
}