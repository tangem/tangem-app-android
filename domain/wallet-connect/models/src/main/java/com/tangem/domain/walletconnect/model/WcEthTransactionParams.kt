package com.tangem.domain.walletconnect.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WcEthTransactionParams(
    @Json(name = "from")
    val from: String,

    @Json(name = "to")
    val to: String?,

    @Json(name = "data")
    val data: String?,

    @Json(name = "gas")
    val gas: String?,

    @Json(name = "gasPrice")
    val gasPrice: String?,

    @Json(name = "value")
    val value: String?,

    @Json(name = "nonce")
    val nonce: String?,

    /**
     * The chain the dApp built this transaction for. WalletConnect does not require it, so it is absent
     * more often than not — but when it is there and names another chain than the request does, the dApp
     * has mixed two networks up and the transaction must not be signed.
     *
     * Hex with a `0x` prefix per EIP-155, though dApps also send a plain decimal number; Moshi reads
     * either into this field.
     */
    @Json(name = "chainId")
    val chainId: String?,
) {

    /** Whether the transaction asks a contract to run something, rather than only moving value. */
    val hasCallData: Boolean
        get() = data?.removePrefix(HEX_PREFIX)?.isNotEmpty() == true

    /** [chainId] as an EIP-155 number, read as hex or decimal the way the dApp wrote it. */
    val declaredChainId: Int?
        get() = when {
            chainId == null -> null
            chainId.startsWith(HEX_PREFIX) -> chainId.removePrefix(HEX_PREFIX).toIntOrNull(radix = HEX_RADIX)
            else -> chainId.toIntOrNull()
        }

    private companion object {
        const val HEX_PREFIX = "0x"
        const val HEX_RADIX = 16
    }
}