package com.tangem.domain.jointaccount.model

import com.tangem.domain.jointaccount.signing.JointAccountSignablePayload

/**
 * The object signed on activation — `payload` of `POST /v1/wallets/{walletId}/joint-accounts/activate`.
 * Signed by the creator's owner key: the backend requires the recovered address to be the creator's.
 *
 * @property walletId        64 hex chars, must equal the `walletId` of the request path
 * @property cryptoAccountId names the account — the caller's own row in `accounts`, 64 upper-case hex chars
 * @property config          the configuration as the account holds it, plus the address it resolved to
 */
data class JointAccountActivationPayload(
    val walletId: String,
    val cryptoAccountId: String,
    val config: ConfirmedConfig,
) : JointAccountSignablePayload {

    /**
     * [JointAccountConfig] plus [safeAddress] — the contract's confirmed config: one field more than the config
     * of joining, because the Safe address does not exist until the last slot is taken.
     *
     * @property safeAddress the address the composition resolved to, recomputed by the app (Safe CREATE2)
     * before signing
     */
    data class ConfirmedConfig(
        val base: JointAccountConfig,
        val safeAddress: String,
    ) {

        internal fun toCanonicalMap(): Map<String, Any> = base.toCanonicalMap() + ("safeAddress" to safeAddress)
    }

    override fun toCanonicalMap(): Map<String, Any> = mapOf(
        "walletId" to walletId,
        "cryptoAccountId" to cryptoAccountId,
        "config" to config.toCanonicalMap(),
    )
}