package com.tangem.datasource.api.tangemTech.models.account

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.utils.SerializeNulls

@JsonClass(generateAdapter = true)
data class SaveWalletAccountsResponse(
    @Json(name = "accounts") val accounts: List<AccountDTO>,
) {

    @SerializeNulls
    @JsonClass(generateAdapter = true)
    data class AccountDTO(
        @Json(name = "id") val id: String,
        @Json(name = "name") val name: String?,
        @Json(name = "derivation") val derivationIndex: Int,
        @Json(name = "icon") val icon: String,
        @Json(name = "iconColor") val iconColor: String,
    )

    companion object {

        operator fun invoke(accounts: List<WalletAccountDTO>): SaveWalletAccountsResponse {
            return SaveWalletAccountsResponse(
                accounts = accounts.map { accountDto ->
                    AccountDTO(
                        id = accountDto.id,
                        name = accountDto.name,
                        derivationIndex = accountDto.derivationIndex,
                        icon = accountDto.icon,
                        iconColor = accountDto.iconColor,
                    )
                },
            )
        }
    }
}