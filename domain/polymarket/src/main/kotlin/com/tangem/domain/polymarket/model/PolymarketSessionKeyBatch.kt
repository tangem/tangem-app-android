package com.tangem.domain.polymarket.model

sealed interface PolymarketSessionKeyBatch {

    val sessionKeyAddress: String

    /**
     * @property replacesSessionKeyAddress an `ACTIVE` key of the same device that Polymarket no longer has
     */
    data class Authorize(
        override val sessionKeyAddress: String,
        val deviceId: String,
        val deviceName: String,
        val replacesSessionKeyAddress: String? = null,
    ) : PolymarketSessionKeyBatch

    data class Revoke(override val sessionKeyAddress: String) : PolymarketSessionKeyBatch
}

/**
 * @property batches in the order they were requested, on consecutive relayer nonces
 * @property ownerClobAuth the owner's ClobAuth, signed in the same session only when the owner had no stored
 * credentials
 */
data class PolymarketSignedSessionKeyBatches(
    val ownerClobAuth: PolymarketSignedOnboarding.SignedClobAuth?,
    val batches: List<Signed>,
) {

    sealed interface Signed {

        data class Authorization(val authorization: PolymarketSessionKeyAuthorization) : Signed

        data class Revocation(val revocation: PolymarketSessionKeyRevocation) : Signed
    }
}