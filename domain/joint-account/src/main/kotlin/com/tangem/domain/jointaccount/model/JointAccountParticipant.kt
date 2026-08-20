package com.tangem.domain.jointaccount.model

/**
 * A participant's identity, bound to the signature — the `creator` of [JointAccountCreationPayload] and the
 * `member` of [JointAccountJoinPayload] share this shape.
 *
 * @property walletId   64 hex chars, must equal the `walletId` of the request path — the signature is bound
 * to the wallet on purpose
 * @property name       the participant's display name, 1..25 chars
 * @property address    owner address derived at `m/44'/60'/888888'/0/{derivation}`, EIP-55 checksummed (EVM)
 * @property derivation derivation index of the owner key
 */
data class JointAccountParticipant(
    val walletId: String,
    val name: String,
    val address: String,
    val derivation: Int,
) {

    internal fun toCanonicalMap(): Map<String, Any> = mapOf(
        "walletId" to walletId,
        "name" to name,
        "address" to address,
        "derivation" to derivation,
    )
}