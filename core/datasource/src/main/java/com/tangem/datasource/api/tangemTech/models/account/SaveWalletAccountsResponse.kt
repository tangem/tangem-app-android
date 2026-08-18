package com.tangem.datasource.api.tangemTech.models.account

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.utils.SerializeNulls

@JsonClass(generateAdapter = true)
data class SaveWalletAccountsResponse(
    @Json(name = "accounts") val accounts: List<AccountDTO>,
) {

    /**
     * @property type `crypto` | `joint`. Always sent explicitly rather than left nullable: the class
     * serializes nulls, and the contract distinguishes an absent field (treated as `crypto`) from a `null` one.
     * A joint row must echo its `type` — omission would collide with the crypto derivation uniqueness check
     */
    @SerializeNulls
    @JsonClass(generateAdapter = true)
    data class AccountDTO(
        @Json(name = "id") val id: String,
        @Json(name = "name") val name: String?,
        @Json(name = "derivation") val derivationIndex: Int,
        @Json(name = "icon") val icon: String,
        @Json(name = "iconColor") val iconColor: String,
        @Json(name = "type") val type: String,
    )

    companion object {

        const val TYPE_CRYPTO = "crypto"

        operator fun invoke(accounts: List<WalletAccountDTO>): SaveWalletAccountsResponse {
            return SaveWalletAccountsResponse(
                accounts = accounts.map { accountDto ->
                    AccountDTO(
                        id = accountDto.id,
                        name = accountDto.name,
                        derivationIndex = accountDto.derivationIndex,
                        icon = accountDto.icon,
                        iconColor = accountDto.iconColor,
                        type = accountDto.type ?: TYPE_CRYPTO,
                    )
                },
            )
        }
    }
}