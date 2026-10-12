package com.tangem.datasource.api.tangemTech.models.account

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetWalletArchivedAccountsResponse(
    @Json(name = "archivedAccounts") val accounts: List<WalletAccountDTO>,
)