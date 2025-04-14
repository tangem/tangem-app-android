package com.tangem.data.walletconnect.model

import com.tangem.data.walletconnect.model.CAIP2.Companion.CAIP_SEPARATOR

/**
 * CAIP-10 defines a way to identify an account in any blockchain specified by [CAIP2] blockchain id.
 *
 * Syntax
 * account_id:        chain_id + ":" + account_address
 * chain_id:          [-a-z0-9]{3,8}:[-_a-zA-Z0-9]{1,32} @see[CAIP2]
 * account_address:   [-.%a-zA-Z0-9]{1,128}
 *
 * Examples
 * # Ethereum mainnet
 * eip155:1:0xab16a96D359eC26a11e2C2b3d8f8B8942d5Bfcdb
 *
 * # Bitcoin mainnet
 * bip122:000000000019d6689c085ae165831e93:128Lkh3S7CkDTBZ8W7BbpsN3YYizJMp8p6
 *
 * For more information https://github.com/ChainAgnostic/CAIPs/blob/main/CAIPs/caip-10.md
 */
internal data class CAIP10 constructor(
    val chainId: CAIP2,
    val accountAddress: String,
) {
    val raw get() = "${chainId.raw}$CAIP_SEPARATOR$accountAddress"

    companion object {
        private const val CAIP10_SUBSTRINGS_SIZE = 3
        fun fromRaw(s: String): CAIP10? {
            val parsed = s.split(CAIP_SEPARATOR)
            if (parsed.size != CAIP10_SUBSTRINGS_SIZE) return null
            val namespace = parsed[0]
            val reference = parsed[1]
            val accountAddress = parsed[2]
            val chainId = CAIP2.fromRaw(namespace + CAIP_SEPARATOR + reference) ?: return null
            return CAIP10(chainId, accountAddress)
        }
    }
}