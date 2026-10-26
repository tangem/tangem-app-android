package com.tangem.domain.jointaccount.model

import com.tangem.domain.jointaccount.signing.JointAccountSignablePayload

/**
 * The object signed on confirmation — `payload` of `POST /v1/wallets/{walletId}/joint-accounts/confirm`.
 *
 * @property safeAddress strictly lower-case: the backend rebuilds this payload from the stored account and
 * compares the canonical JSON byte by byte
 */
data class JointAccountConfirmationPayload(
    val config: JointAccountConfig,
    val safeAddress: String,
) : JointAccountSignablePayload {

    override fun toCanonicalMap(): Map<String, Any> = config.toCanonicalMap() + ("safeAddress" to safeAddress)
}