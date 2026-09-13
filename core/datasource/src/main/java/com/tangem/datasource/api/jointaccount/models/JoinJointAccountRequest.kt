package com.tangem.datasource.api.jointaccount.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for `POST /v1/wallets/{walletId}/joint-accounts/join`.
 *
 * [signature] is an EIP-191 (`0x45`) signature over [payload] serialized as RFC 8785 canonical JSON,
 * `0x` + 130 hex chars (65 bytes R‖S‖V, v ∈ {27, 28}); the recovered address must equal
 * [JointAccountParticipantDto.address] of the member.
 */
@JsonClass(generateAdapter = true)
data class JoinJointAccountRequest(
    @Json(name = "payload") val payload: Payload,
    @Json(name = "signature") val signature: String,
) {

    /**
     * @property inviteId the invite being redeemed, 64 upper-case hex chars. Carried inside the signed payload
     * rather than in the path, because the signature has to cover it — an invite that only appeared in the URL
     * could be swapped for another one
     * @property config   verbatim from [JointAccountInvitePreviewDto.config]; must match what the account holds
     * @property member   the joiner's identity, bound to the signature
     */
    @JsonClass(generateAdapter = true)
    data class Payload(
        @Json(name = "inviteId") val inviteId: String,
        @Json(name = "config") val config: JointAccountConfigDto,
        @Json(name = "member") val member: JointAccountParticipantDto,
    )
}