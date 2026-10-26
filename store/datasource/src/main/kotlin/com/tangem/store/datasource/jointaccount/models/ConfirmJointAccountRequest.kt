package com.tangem.store.datasource.jointaccount.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for `POST /v1/wallets/{walletId}/joint-accounts/confirm`.
 *
 * [signature] is an EIP-191 (`0x45`) signature over [payload] serialized as RFC 8785 canonical JSON,
 * `0x` + 130 hex chars (65 bytes R‖S‖V, v ∈ {27, 28}); the recovered address must be the owner address of
 * this wallet's slot — every member confirms, the creator included.
 */
@JsonClass(generateAdapter = true)
data class ConfirmJointAccountRequest(
    @Json(name = "payload") val payload: Payload,
    @Json(name = "signature") val signature: String,
) {

    /**
     * Compared with the stored account byte by byte, so every value must be exactly what `GET` returned.
     *
     * @property safeAddress strictly lower-case — a checksummed copy is a 400
     */
    @JsonClass(generateAdapter = true)
    data class Payload(
        @Json(name = "name") val name: String,
        @Json(name = "icon") val icon: String,
        @Json(name = "iconColor") val iconColor: String,
        @Json(name = "membersCount") val membersCount: Int,
        @Json(name = "threshold") val threshold: Int,
        @Json(name = "safeAddress") val safeAddress: String,
    )
}