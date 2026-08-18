package com.tangem.datasource.api.jointaccount.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for `POST /v1/wallets/{walletId}/joint-accounts`.
 *
 * [signature] is an EIP-191 (`0x45`) signature over [payload] serialized as RFC 8785 canonical JSON,
 * `0x` + 130 hex chars (65 bytes R‖S‖V, v ∈ {27, 28}). The backend canonicalizes the received
 * [payload] as is and requires the recovered address to equal [Payload.Creator.address].
 */
@JsonClass(generateAdapter = true)
data class CreateJointAccountRequest(
    @Json(name = "payload") val payload: Payload,
    @Json(name = "signature") val signature: String,
) {

    @JsonClass(generateAdapter = true)
    data class Payload(
        @Json(name = "config") val config: Config,
        @Json(name = "creator") val creator: Creator,
    )

    /**
     * Shared account config, immutable once the account exists.
     *
     * @property name         account name, 1..20 chars, not normalized
     * @property icon         icon name, from the same set as crypto accounts
     * @property iconColor    color name, from the same set as crypto accounts
     * @property membersCount slots including the creator, 2..5
     * @property threshold    signatures required to execute an operation, 1..[membersCount]
     */
    @JsonClass(generateAdapter = true)
    data class Config(
        @Json(name = "name") val name: String,
        @Json(name = "icon") val icon: String,
        @Json(name = "iconColor") val iconColor: String,
        @Json(name = "membersCount") val membersCount: Int,
        @Json(name = "threshold") val threshold: Int,
    )

    /**
     * The creator's identity, bound to the signature.
     *
     * @property walletId   64 hex chars, must equal the `walletId` in the path
     * @property name       display name, 1..25 chars, no special characters, not normalized
     * @property address    owner address derived at `m/44'/60'/888888'/0/{derivation}`, `0x` + 40 hex
     * @property derivation derivation index of the owner key, >= 0
     */
    @JsonClass(generateAdapter = true)
    data class Creator(
        @Json(name = "walletId") val walletId: String,
        @Json(name = "name") val name: String,
        @Json(name = "address") val address: String,
        @Json(name = "derivation") val derivation: Int,
    )
}