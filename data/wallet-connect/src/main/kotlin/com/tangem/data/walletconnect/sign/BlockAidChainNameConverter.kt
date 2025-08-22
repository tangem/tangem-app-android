package com.tangem.data.walletconnect.sign

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.domain.models.network.Network
import com.tangem.utils.converter.Converter

internal object BlockAidChainNameConverter : Converter<Network, String?> {

    @Suppress("CyclomaticComplexMethod")
    override fun convert(value: Network): String? {
        return when (Blockchain.fromNetworkId(value.backendId)) {
            Blockchain.Arbitrum -> "arbitrum"
            Blockchain.Avalanche -> "avalanche"
            Blockchain.AvalancheTestnet -> "avalanche-fuji"
            Blockchain.Base -> "base"
            Blockchain.BaseTestnet -> "base-sepolia"
            Blockchain.Binance, Blockchain.BSC -> "bsc"
            Blockchain.Ethereum -> "ethereum"
            Blockchain.Optimism -> "optimism"
            Blockchain.Polygon -> "polygon"
            Blockchain.ZkSyncEra -> "zksync"
            Blockchain.ZkSyncEraTestnet -> "zksync-sepolia"
            Blockchain.Blast, Blockchain.BlastTestnet -> "blast"
            Blockchain.Scroll -> "scroll"
            Blockchain.EthereumTestnet -> "ethereum-sepolia"
            Blockchain.Gnosis -> "gnosis"
            Blockchain.ApeChain, Blockchain.ApeChainTestnet -> "apechain"

            Blockchain.Solana -> "mainnet"

            else -> null
        }
    }
}