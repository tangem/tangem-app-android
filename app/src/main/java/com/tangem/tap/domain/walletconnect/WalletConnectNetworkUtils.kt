package com.tangem.tap.domain.walletconnect

import com.tangem.blockchain.common.Blockchain
import com.trustwallet.walletconnect.models.WCPeerMeta

class WalletConnectNetworkUtils {

    companion object {

        fun parseBlockchain(
            chainId: Int?,
            peer: WCPeerMeta,
            isTestNet: Boolean? = null
        ): Blockchain? {
            return when {
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
                else -> {
                    if (isTestNet == true) Blockchain.EthereumTestnet else Blockchain.Ethereum
                }
            }
        }

    }
}