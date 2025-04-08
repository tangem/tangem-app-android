package com.tangem.data.walletconnect.model

/**
 * CAIP-2 defines a way to identify a blockchain (e.g. Ethereum Mainnet, Görli, Bitcoin, Cosmos Hub)
 * in a human-readable, developer-friendly and transaction-friendly way.
 *
 * Syntax
 * chain_id:    namespace + ":" + reference
 * namespace:   [-a-z0-9]{3,8}
 * reference:   [-_a-zA-Z0-9]{1,32}
 *
 * Examples
 * # Ethereum mainnet
 * eip155:1
 *
 * # Bitcoin mainnet
 * bip122:000000000019d6689c085ae165831e93
 *
 * For more information https://github.com/ChainAgnostic/CAIPs/blob/main/CAIPs/caip-2.md
 */
internal data class CAIP2 constructor(
    val namespace: String,
    val reference: String,
) {
    val raw get() = "$namespace$CAIP_SEPARATOR$reference"

    companion object {
        const val CAIP_SEPARATOR = ":"

        fun fromRaw(s: String): CAIP2? {
            val parsed = s.split(CAIP_SEPARATOR)
            if (parsed.size != 2) return null
            return CAIP2(parsed[0], parsed[1])
        }
    }
}