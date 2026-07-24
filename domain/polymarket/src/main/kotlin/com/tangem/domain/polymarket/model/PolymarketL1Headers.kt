package com.tangem.domain.polymarket.model

/**
 * The four L1 auth headers a CLOB `auth` call requires, produced from an EIP-712 signature made by
 * the card. Kept as a single object so the header names live in one place and callers cannot transpose them.
 */
data class PolymarketL1Headers(
    val address: String,
    val signature: String,
    val timestamp: String,
    val nonce: String,
) {

    fun toMap(): Map<String, String> = mapOf(
        HEADER_ADDRESS to address,
        HEADER_SIGNATURE to signature,
        HEADER_TIMESTAMP to timestamp,
        HEADER_NONCE to nonce,
    )

    override fun toString(): String =
        "PolymarketL1Headers(address=$address, signature=$REDACTED, timestamp=$timestamp, nonce=$REDACTED)"

    private companion object {
        const val HEADER_ADDRESS = "POLY_ADDRESS"
        const val HEADER_SIGNATURE = "POLY_SIGNATURE"
        const val HEADER_TIMESTAMP = "POLY_TIMESTAMP"
        const val HEADER_NONCE = "POLY_NONCE"
        const val REDACTED = "REDACTED"
    }
}