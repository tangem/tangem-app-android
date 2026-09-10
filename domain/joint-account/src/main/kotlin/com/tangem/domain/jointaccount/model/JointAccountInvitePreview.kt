package com.tangem.domain.jointaccount.model

/**
 * What the invitee is joining: the configuration and who is inviting. The other participants are not listed.
 *
 * @property config  copied verbatim into the `payload.config` of [JointAccountJoinPayload] — the backend rejects
 * a join whose config does not match what the account holds
 * @property creator who is inviting
 */
data class JointAccountInvitePreview(
    val config: JointAccountConfig,
    val creator: Creator,
) {

    /**
     * @property name    display name, 1..25 chars
     * @property address the creator's owner address, lower-case hex — compare case-insensitively
     */
    data class Creator(
        val name: String,
        val address: String,
    )
}