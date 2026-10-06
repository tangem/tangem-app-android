package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.common.extensions.calculateHashCode
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse.GroupType
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse.GroupType.NONE
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse.SortType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@JsonClass(generateAdapter = true)
@Suppress("BooleanPropertyNaming")
data class UserTokensResponse(
    @Json(name = "version") val version: Int = 0,
    @Json(name = "group") val group: GroupType,
    @Json(name = "sort") val sort: SortType,
    @Json(name = "tokens") val tokens: List<Token> = emptyList(),
    @Json(name = "notifyStatus") val notifyStatus: Boolean? = null,
    @Json(name = "name") val walletName: String? = null,
    @Json(name = "type") val walletType: WalletType? = null,
    @Json(name = "ref") val refcode: String? = null,
    @Json(name = "campaign") val campaign: String? = null,
) {

    @Serializable
    @JsonClass(generateAdapter = true)
    data class Token(
        @Json(name = "id") val id: String? = null,
        @Json(name = "accountId") val accountId: String? = null,
        @Json(name = "networkId") val networkId: String,
        @Json(name = "derivationPath") val derivationPath: String? = null,
        @Json(name = "name") val name: String,
        @Json(name = "symbol") val symbol: String,
        @Json(name = "decimals") val decimals: Int,
        @Json(name = "contractAddress") val contractAddress: String?,
        @Json(name = "addresses") val addresses: List<String>? = null,
        @Json(name = "dynamicAddressesEnabled") val dynamicAddressesEnabled: Boolean? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            val otherToken = other as? Token ?: return false

            // use only this fields is enough and correct to compare, don't add id or smth
            // to avoid duplicates for tokens on main screen
            return otherToken.contractAddress == this.contractAddress &&
                otherToken.networkId == this.networkId &&
                otherToken.derivationPath == this.derivationPath &&
                otherToken.decimals == this.decimals &&
                otherToken.dynamicAddressesEnabled == this.dynamicAddressesEnabled
        }

        override fun hashCode(): Int = calculateHashCode(
            contractAddress.hashCode(),
            networkId.hashCode(),
            derivationPath.hashCode(),
            decimals.hashCode(),
            dynamicAddressesEnabled.hashCode(),
        )
    }

    @Serializable
    @JsonClass(generateAdapter = false)
    enum class GroupType {
        @SerialName("none")
        @Json(name = "none")
        NONE,

        @SerialName("token")
        @Json(name = "token")
        TOKEN,

        @SerialName("network")
        @Json(name = "network")
        NETWORK,
    }

    @Serializable
    @JsonClass(generateAdapter = false)
    enum class SortType {
        @SerialName("balance")
        @Json(name = "balance")
        BALANCE,

        @SerialName("manual")
        @Json(name = "manual")
        MANUAL,

        @SerialName("marketcap")
        @Json(name = "marketcap")
        MARKETCAP,
    }
}

fun GroupType?.orDefault(): GroupType = this ?: NONE

fun SortType?.orDefault(): SortType = this ?: SortType.MANUAL