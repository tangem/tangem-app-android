package com.tangem.data.tangempay.entity

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
     */
    private fun resolveBlockchain(name: String, chainId: Long): Blockchain? {
        Blockchain.fromChainId(chainId)?.let { return it }
        return when (name.lowercase(Locale.US)) {
            "tron" -> Blockchain.Tron
            else -> null
        }
    }
}