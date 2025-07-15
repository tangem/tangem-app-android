package com.tangem.data.walletconnect.sign

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.domain.models.network.Network
import com.tangem.utils.converter.Converter
import javax.inject.Inject

internal class BlockAidChainNameConverter @Inject constructor() : Converter<Network, String> {

    @Suppress("CyclomaticComplexMethod")
    override fun convert(value: Network): String {
        return when (Blockchain.fromNetworkId(value.backendId)) {
            Blockchain.Arbitrum -> "arbitrum"
            Blockchain.Avalanche -> "avalanche"
            Blockchain.AvalancheTestnet -> "avalanche-fuji"
            Blockchain.Binance, Blockchain.BSC -> "bsc"
            Blockchain.Ethereum -> "ethereum"
            Blockchain.EthereumTestnet -> "ethereum-sepolia"
            Blockchain.Polygon -> "polygon"
            Blockchain.Solana -> "mainnet"
            Blockchain.Gnosis -> "gnosis"
            Blockchain.Optimism -> "optimism"
            Blockchain.ZkSyncEra -> "zksync"
            Blockchain.ZkSyncEraTestnet -> "zksync-sepolia"
            Blockchain.Base -> "base"
            Blockchain.BaseTestnet -> "base-sepolia"
            Blockchain.Blast, Blockchain.BlastTestnet -> "blast"
            Blockchain.ApeChain, Blockchain.ApeChainTestnet -> "apechain"
            Blockchain.Scroll -> "scroll"
            else -> value.name
        }
    }
}