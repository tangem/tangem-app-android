package com.tangem.datasource.api.tangemTech.models.account

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse

/**
 * @property type `crypto` | `joint`; absent means `crypto` — the field predates joint accounts.
 * Kept as a raw string: mapping to a domain type is the data layer's job, a parse must never fail on a value
 * the backend adds later
 */
@JsonClass(generateAdapter = true)
data class WalletAccountDTO(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String?,
    @Json(name = "derivation") val derivationIndex: Int,
    @Json(name = "icon") val icon: String,
    @Json(name = "iconColor") val iconColor: String,
    @Json(name = "type") val type: String? = null,
    @Json(name = "tokens") val tokens: List<UserTokensResponse.Token>? = null,
    @Json(name = "totalTokens") val totalTokens: Int? = null,
    @Json(name = "totalNetworks") val totalNetworks: Int? = null,
) {

    companion object {
        const val TYPE_CRYPTO = "crypto"
        const val TYPE_JOINT = "joint"
    }
}