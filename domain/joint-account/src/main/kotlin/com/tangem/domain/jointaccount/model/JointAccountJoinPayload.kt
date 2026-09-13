package com.tangem.domain.jointaccount.model

import com.tangem.domain.jointaccount.signing.JointAccountSignablePayload

/**
 * The object signed on joining — `payload` of `POST /v1/wallets/{walletId}/joint-accounts/join`.
 *
 * @property inviteId the invite being redeemed, 64 upper-case hex chars. Carried inside the signed payload
 * rather than in the URL, because the signature has to cover it — an invite that only appeared in the URL
 * could be swapped for another one
 * @property config   verbatim from the invite preview; a config that differs from what the account holds is a 409
 * @property member   the joiner's identity, bound to the signature
 */
data class JointAccountJoinPayload(
    val inviteId: String,
    val config: JointAccountConfig,
    val member: JointAccountParticipant,
) : JointAccountSignablePayload {

    override fun toCanonicalMap(): Map<String, Any> = mapOf(
        "inviteId" to inviteId,
        "config" to config.toCanonicalMap(),
        "member" to member.toCanonicalMap(),
    )
}