package com.tangem.domain.jointaccount.model

import com.tangem.domain.jointaccount.signing.JointAccountSignablePayload

/**
 * The object signed on activation — `payload` of `POST /v1/wallets/{walletId}/joint-accounts/activate`.
 * Signed by the creator's owner key: the backend requires the recovered address to be the creator's.
 *
 * @property walletId        64 hex chars, must equal the `walletId` of the request path
 * @property cryptoAccountId names the account — the caller's own row in `accounts`, 64 upper-case hex chars
 * @property config          the configuration as the account holds it
 * @property safeAddress     the address the composition resolved to, recomputed by the app (Safe CREATE2)
 * before signing; goes into the canonical form as one more `config` field, per the contract's confirmed config
 */
data class JointAccountActivationPayload(
    val walletId: String,
    val cryptoAccountId: String,
    val config: JointAccountConfig,
    val safeAddress: String,
) : JointAccountSignablePayload {

    override fun toCanonicalMap(): Map<String, Any> = mapOf(
        "walletId" to walletId,
        "cryptoAccountId" to cryptoAccountId,
        "config" to config.toCanonicalMap() + ("safeAddress" to safeAddress),
    )
}