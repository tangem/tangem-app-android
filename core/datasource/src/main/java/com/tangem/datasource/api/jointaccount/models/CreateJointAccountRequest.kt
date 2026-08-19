package com.tangem.datasource.api.jointaccount.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for `POST /v1/wallets/{walletId}/joint-accounts`.
 *
 * [signature] is an EIP-191 (`0x45`) signature over [payload] serialized as RFC 8785 canonical JSON,
 * `0x` + 130 hex chars (65 bytes R‖S‖V, v ∈ {27, 28}). The backend canonicalizes the received
 * [payload] as is and requires the recovered address to equal [JointAccountParticipantDto.address]
 * of the creator.
 */
@JsonClass(generateAdapter = true)
data class CreateJointAccountRequest(
    @Json(name = "payload") val payload: Payload,
    @Json(name = "signature") val signature: String,
) {

    @JsonClass(generateAdapter = true)
    data class Payload(
        @Json(name = "config") val config: JointAccountConfigDto,
        @Json(name = "creator") val creator: JointAccountParticipantDto,
    )
}