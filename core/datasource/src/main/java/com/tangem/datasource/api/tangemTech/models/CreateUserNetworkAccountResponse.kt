package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateUserNetworkAccountResponse(
    @Json(name = "status") val status: String,
    @Json(name = "data") val data: AccountCreated,
) {

    @JsonClass(generateAdapter = true)
    data class AccountCreated(
        @Json(name = "accountId") val accountId: String,
        @Json(name = "publicWalletKey") val publicWalletKey: String,
    )
}
