package com.tangem.data.walletconnect.network.solana

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.walletconnect.model.CAIP2
import com.tangem.data.walletconnect.model.NamespaceKey
import com.tangem.data.walletconnect.utils.WcNamespaceConverter
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWallet

internal class WcSolanaNetwork constructor(
    private val excludedBlockchains: ExcludedBlockchains,
) : WcNamespaceConverter {

    override val namespaceKey: NamespaceKey = NamespaceKey("solana")

    override fun toBlockchain(chainId: CAIP2): Blockchain? {
        if (chainId.namespace != namespaceKey.key) return null
        return when (chainId.reference) {
            MAINNET_CHAIN_ID -> Blockchain.Solana
            TESTNET_CHAIN_ID -> Blockchain.SolanaTestnet
            else -> null
        }
    }

    override fun toNetwork(chainId: String, wallet: UserWallet): Network? {
        return toNetwork(chainId, wallet, excludedBlockchains)
    }

    override fun toCAIP2(network: Network): CAIP2? {
        val blockchain = Blockchain.fromId(network.id.value)
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