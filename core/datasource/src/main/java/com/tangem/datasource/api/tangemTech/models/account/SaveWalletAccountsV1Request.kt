package com.tangem.datasource.api.tangemTech.models.account

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.utils.SerializeNulls

/**
 * The body of `PUT /v1/wallets/{walletId}/accounts`.
 *
 * `v1` predates joint accounts and knows nothing of a record type, so the field is not part of this shape at all
 * rather than nullable — the class serializes nulls for [AccountDTO.name], and a `"type": null` would reach the
 * backend as a field it does not expect. The mapping drops the field and nothing else: which records reach `v1` is
 * decided where the accounts document is fetched, not here.
 */
@JsonClass(generateAdapter = true)
data class SaveWalletAccountsV1Request(
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

        fun of(body: SaveWalletAccountsResponse): SaveWalletAccountsV1Request {
            return SaveWalletAccountsV1Request(
                accounts = body.accounts.map { account ->
                    AccountDTO(
                        id = account.id,
                        name = account.name,
                        derivationIndex = account.derivationIndex,
                        icon = account.icon,
                        iconColor = account.iconColor,
                    )
                },
            )
        }
    }
}