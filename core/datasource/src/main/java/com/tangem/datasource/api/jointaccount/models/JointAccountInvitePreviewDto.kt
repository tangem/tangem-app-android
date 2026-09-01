package com.tangem.datasource.api.jointaccount.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response of `GET /v1/wallets/{walletId}/joint-accounts/invites/{inviteId}` — what the invitee is joining:
 * the configuration and who is inviting. The other participants are not listed.
 *
 * @property config  copied verbatim into the `payload.config` of [JoinJointAccountRequest] — the backend answers
 * 409 to a join whose config differs from what the account holds
 * @property creator who is inviting
 */
@JsonClass(generateAdapter = true)
data class JointAccountInvitePreviewDto(
    @Json(name = "config") val config: JointAccountConfigDto,
    @Json(name = "creator") val creator: Creator,
) {

    /**
     * @property name    display name, 1..25 chars
     * @property address the creator's owner address, EIP-55 checksummed
     */
    @JsonClass(generateAdapter = true)
    data class Creator(
        @Json(name = "name") val name: String,
        @Json(name = "address") val address: String,
    )
}