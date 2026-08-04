package com.tangem.data.pay.entity

import com.tangem.blockchain.common.Blockchain
import java.util.Locale

/**
 * Resolves a backend payment-account network (name + numeric chain id + testnet flag) to a Tangem [Blockchain].
 *
 * EVM networks resolve via [Blockchain.fromChainId]. Non-EVM launch networks (Tron) are not in the
 * EVM chain table, so they are matched by name. When [isTestnet] is true the resolved blockchain is
 * mapped to its testnet variant via [Blockchain.getTestnetVersion]; if the chain has no testnet variant
 * the resolved blockchain is kept as-is. Returns `null` for anything unrecognised — the caller keeps the
 * network in its metadata list but does not build a spendable currency for it.
 */
internal object PaymentNetworkBlockchainResolver {

    fun blockchainFor(name: String, chainId: Long, isTestnet: Boolean): Blockchain? {
        val blockchain = resolveBlockchain(name, chainId) ?: return null
        return if (isTestnet) blockchain.getTestnetVersion() else blockchain
    }

    /**
     * Resolves the mainnet [Blockchain] by chain id (EVM) or by name (non-EVM Tron), ignoring the testnet flag.
     *
     * Guards against a Long -> Int overflow: [Blockchain.fromChainId] takes an `Int`, and `chainId.toInt()`
     * silently wraps for values outside the Int range, which could collide with a real EVM chain id
     * (e.g. `137 + 2^32` wraps back to Polygon's `137`) and build a token on the wrong network — so
     * `fromChainId` is only consulted when the chain id fits in an `Int`.
     */
    private fun resolveBlockchain(name: String, chainId: Long): Blockchain? {
        if (chainId in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
            Blockchain.fromChainId(chainId.toInt())?.let { return it }
        }
        return when (name.lowercase(Locale.US)) {
            "tron" -> Blockchain.Tron
            else -> null
        }
    }
}