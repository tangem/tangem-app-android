package com.tangem.tap.common.extensions

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.remove

fun Blockchain.getNetworkName(): String {
    return when (this) {
        Blockchain.Ethereum, Blockchain.EthereumTestnet -> "ERC20"
        Blockchain.BSC, Blockchain.BSCTestnet -> "BEP20"
        Blockchain.Binance, Blockchain.BinanceTestnet -> "BEP2"
        Blockchain.Tron, Blockchain.TronTestnet -> "TRC20"
        else -> ""
    }
}

val Blockchain.fullNameWithoutTestnet
    get() = this.fullName.remove(" Testnet")