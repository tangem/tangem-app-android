package com.tangem.datasource.api.jointaccount.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for `POST /v1/wallets/{walletId}/joint-accounts/activate`.
 *
 * [signature] is an EIP-191 (`0x45`) signature over [payload] serialized as RFC 8785 canonical JSON,
 * `0x` + 130 hex chars (65 bytes R‖S‖V, v ∈ {27, 28}); the recovered address must be the creator's
 * owner address.
 */
@JsonClass(generateAdapter = true)
data class ActivateJointAccountRequest(
    @Json(name = "payload") val payload: Payload,
    @Json(name = "signature") val signature: String,
) {

    /**
     * @property walletId        64 hex chars, must equal the `walletId` in the path
     * @property cryptoAccountId names the account — the caller's own row in `accounts`, 64 upper-case hex chars
     * @property config          the configuration as the account holds it, plus the address it resolved to
     */
    @JsonClass(generateAdapter = true)
    data class Payload(
        @Json(name = "walletId") val walletId: String,
        @Json(name = "cryptoAccountId") val cryptoAccountId: String,
        @Json(name = "config") val config: ConfirmedConfig,
    )

    /**
     * [JointAccountConfigDto] plus [safeAddress] — one field more than the config of joining, because the Safe
     * address does not exist until the last slot is taken. Anything that differs from what the account holds is 409.
     *
     * @property safeAddress recomputed by the app from the composition (Safe CREATE2), `0x` + 40 hex
     */
    @JsonClass(generateAdapter = true)
    data class ConfirmedConfig(
        @Json(name = "name") val name: String,
        @Json(name = "icon") val icon: String,
        @Json(name = "iconColor") val iconColor: String,
        @Json(name = "membersCount") val membersCount: Int,
        @Json(name = "threshold") val threshold: Int,
        @Json(name = "safeAddress") val safeAddress: String,
    )
}