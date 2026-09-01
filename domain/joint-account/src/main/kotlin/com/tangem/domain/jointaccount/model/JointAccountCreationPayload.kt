package com.tangem.domain.jointaccount.model

import com.tangem.domain.jointaccount.signing.JointAccountSignablePayload

/**
 * The object signed on joint account creation — `payload` of `POST /v1/wallets/{walletId}/joint-accounts`.
 */
data class JointAccountCreationPayload(
    val config: JointAccountConfig,
    val creator: JointAccountParticipant,
) : JointAccountSignablePayload {

    override fun toCanonicalMap(): Map<String, Any> = mapOf(
        "config" to config.toCanonicalMap(),
        "creator" to creator.toCanonicalMap(),
    )
}