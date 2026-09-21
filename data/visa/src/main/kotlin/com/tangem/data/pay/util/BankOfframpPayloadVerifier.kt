package com.tangem.data.pay.util

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.common.extensions.toHexString

/**
 * Tells whether an order's hash really is the EIP-712 digest of the typed data the backend sent with it.
 *
 * The card signs 32 opaque bytes, so this is the only moment the app can tell what it is about to authorise: if
 * the two disagree, the hash describes a transfer nobody has seen and the order must not be signed.
 */
internal object BankOfframpPayloadVerifier {

    fun isConsistent(hash: String, structuredData: String): Boolean = runCatching {
        EthereumUtils.makeTypedDataHash(rawMessage = structuredData)
            .toHexString()
            .equals(hash.withoutHexPrefix(), ignoreCase = true)
    }.getOrDefault(defaultValue = false)

    private fun String.withoutHexPrefix(): String =
        if (startsWith(prefix = HEX_PREFIX, ignoreCase = true)) drop(n = HEX_PREFIX.length) else this
}