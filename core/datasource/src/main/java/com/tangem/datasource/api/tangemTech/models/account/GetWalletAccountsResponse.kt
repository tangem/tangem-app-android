package com.tangem.datasource.api.tangemTech.models.account

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse.GroupType
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse.SortType

@JsonClass(generateAdapter = true)
data class GetWalletAccountsResponse(
    @Json(name = "wallet") val wallet: Wallet,
    @Json(name = "accounts") val accounts: List<WalletAccountDTO>,
    @Json(name = "unassignedTokens") val unassignedTokens: List<UserTokensResponse.Token>,
) {

    @JsonClass(generateAdapter = true)
    data class Wallet(
        @Json(name = "version") val version: Int,
        @Json(name = "group") val group: GroupType,
        @Json(name = "sort") val sort: SortType,
        @Json(name = "totalAccounts") val totalAccounts: Int,
    )
}