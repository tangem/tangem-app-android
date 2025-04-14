package com.tangem.data.walletconnect.network.solana

import com.tangem.blockchain.common.Blockchain
import com.tangem.data.walletconnect.model.CAIP2
import com.tangem.data.walletconnect.model.NamespaceKey
import com.tangem.data.walletconnect.utils.WcNamespaceConverter

internal class WcSolanaNetwork : WcNamespaceConverter {
    override val namespaceKey: NamespaceKey = NamespaceKey("solana")

    override fun toBlockchain(chainId: CAIP2): Blockchain? {
        if (chainId.namespace != namespaceKey.key) return null
        return when (chainId.reference) {
            MAINNET_CHAIN_ID -> Blockchain.Solana
            TESTNET_CHAIN_ID -> Blockchain.SolanaTestnet
            else -> null
        }
    }

    override fun toCAIP2(blockchain: Blockchain): CAIP2? {
        val chainId = when (blockchain) {
            Blockchain.Solana -> MAINNET_CHAIN_ID
            Blockchain.SolanaTestnet -> TESTNET_CHAIN_ID
            else -> null
        }
        chainId ?: return null
        return CAIP2(
            namespace = namespaceKey.key,
            reference = chainId,
        )
    }

    companion object {
        private const val MAINNET_CHAIN_ID = "5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp"
        private const val TESTNET_CHAIN_ID = "4uhcVJyU9pJkvQyS88uRDiswHXSCkY3z"
    }
}