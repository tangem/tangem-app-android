package com.tangem.datasource.api.tangemTech.models.account

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse.GroupType
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse.SortType

/**
 * The document of `GET` / `PUT /api/v2/wallets/{walletId}/accounts`.
 *
 * `api/v2` counts the wallet's own accounts as `totalCryptoAccounts` where `v1` — and the persisted document, which
 * keeps the `v1` names — says `totalAccounts`; `v1` has no `totalJointAccounts` at all. The two versions differ in
 * nothing else, so [toResponse] renames the counters and passes the rest through.
 */
@JsonClass(generateAdapter = true)
data class GetWalletAccountsV2Response(
    @Json(name = "wallet") val wallet: Wallet,
    @Json(name = "accounts") val accounts: List<WalletAccountDTO>,
    @Json(name = "unassignedTokens") val unassignedTokens: List<UserTokensResponse.Token>,
) {

    @JsonClass(generateAdapter = true)
    data class Wallet(
        @Json(name = "version") val version: Int? = 0,
        @Json(name = "group") val group: GroupType?,
        @Json(name = "sort") val sort: SortType?,
        @Json(name = "totalCryptoAccounts") val totalCryptoAccounts: Int,
        @Json(name = "totalArchivedAccounts") val totalArchivedAccounts: Int,
        @Json(name = "totalJointAccounts") val totalJointAccounts: Int,
    )

    fun toResponse(): GetWalletAccountsResponse {
        return GetWalletAccountsResponse(
            wallet = GetWalletAccountsResponse.Wallet(
                version = wallet.version,
                group = wallet.group,
                sort = wallet.sort,
                totalAccounts = wallet.totalCryptoAccounts,
                totalArchivedAccounts = wallet.totalArchivedAccounts,
                totalJointAccounts = wallet.totalJointAccounts,
            ),
            accounts = accounts,
            unassignedTokens = unassignedTokens,
        )
    }
}