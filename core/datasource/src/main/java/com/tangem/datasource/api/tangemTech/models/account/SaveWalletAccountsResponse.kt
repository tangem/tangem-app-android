package com.tangem.datasource.api.tangemTech.models.account

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SaveWalletAccountsResponse(
    @Json(name = "accounts") val accounts: List<WalletAccountDTO>,
)