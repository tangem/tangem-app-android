package com.tangem.datasource.api.tangemTech.models.account

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse

/**
 * @property type `crypto` | `joint`; absent means `crypto` — the field predates joint accounts.
 * Kept as a raw string: a parse must never fail on a value the backend adds later. Map it with
 * [WalletAccountDTO.Type.of], which names the values this build knows and answers `null` for the rest
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

    /**
     * The account types this build knows, as they appear in [WalletAccountDTO.type].
     *
     * @property value the raw value on the wire
     */
    enum class Type(val value: String) {
        CRYPTO(value = "crypto"),
        JOINT(value = "joint"),
        ;

        companion object {

            /**
             * The type [raw] names, or `null` when it names none — an absent value (the field predates joint
             * accounts) or one this build has never heard of.
             */
            fun of(raw: String?): Type? = entries.firstOrNull { type -> type.value == raw }
        }
    }
}