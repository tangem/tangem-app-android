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
        @Json(name = "version") val version: Int? = 0,
        @Json(name = "group") val group: GroupType?,
        @Json(name = "sort") val sort: SortType?,
        @Json(name = "totalAccounts") val totalAccounts: Int,
        @Json(name = "totalArchivedAccounts") val totalArchivedAccounts: Int,
    )
}

/** Flattens the tokens from all wallet accounts into a single list */
fun GetWalletAccountsResponse.flattenTokens(): List<UserTokensResponse.Token> {
    return accounts.flatMap { it.tokens.orEmpty() }
}

/** Converts the [GetWalletAccountsResponse] into a [UserTokensResponse] */
fun GetWalletAccountsResponse.toUserTokensResponse(): UserTokensResponse {
    return UserTokensResponse(
        group = wallet.group ?: GroupType.NONE,
        sort = wallet.sort ?: SortType.MANUAL,
        tokens = flattenTokens(),
    )
}