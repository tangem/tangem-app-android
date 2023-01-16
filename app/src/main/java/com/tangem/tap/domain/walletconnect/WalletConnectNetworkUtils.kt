package com.tangem.tap.domain.walletconnect

import com.tangem.blockchain.common.Blockchain
import com.trustwallet.walletconnect.models.WCPeerMeta

object WalletConnectNetworkUtils {
    fun parseBlockchain(chainId: Int?, peer: WCPeerMeta): Blockchain? {
        return when {
            peer.url.contains("pancakeswap.finance") -> {
                Blockchain.BSC
            }
            peer.url.contains("optimism") -> {
                Blockchain.Optimism
            }
            chainId != null -> {
                Blockchain.fromChainId(chainId)
            }
            peer.url.contains("matic.network") || peer.name == "Polygon" -> {
                Blockchain.Polygon
            }
            peer.url.contains("binance.org") || peer.name.contains("Binance") -> {
                if (peer.icons.firstOrNull()?.contains("testnet") == true) {
                    Blockchain.BinanceTestnet
                } else {
                    Blockchain.Binance
                }
            }
            peer.name.contains("BSC") -> {
                Blockchain.BSC
            }
            peer.url.contains("honeyswap.1hive.eth.limo") -> {
                // Check if something's changed after this bug report:
                // https://github.com/1Hive/honeyswap-interface/issues/83
                Blockchain.Gnosis
            }
            else -> {
                Blockchain.Ethereum
            }
        }
    }
}
