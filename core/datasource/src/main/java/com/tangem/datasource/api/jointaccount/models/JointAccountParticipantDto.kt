package com.tangem.datasource.api.jointaccount.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * A participant's identity, bound to the signature — the `creator` of [CreateJointAccountRequest] and the `member`
 * of [JoinJointAccountRequest] share this shape.
 *
 * @property walletId   64 hex chars, must equal the `walletId` in the path — duplicated on purpose: the path is the
 * routing key, and the signature has to be bound to the wallet
 * @property name       display name, 1..25 chars, no special characters, not normalized
 * @property address    owner address derived at `m/44'/60'/888888'/0/{derivation}`, `0x` + 40 hex
 * @property derivation derivation index of the owner key, counted in this wallet, >= 0
 */
@JsonClass(generateAdapter = true)
data class JointAccountParticipantDto(
    @Json(name = "walletId") val walletId: String,
    @Json(name = "name") val name: String,
    @Json(name = "address") val address: String,
    @Json(name = "derivation") val derivation: Int,
)