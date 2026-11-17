package com.tangem.lib.crypto

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network

fun CryptoCurrency.receiveNetworkLabel(): String? {
    network.exchangeAlias()?.let { return it }

    return when (this) {
        is CryptoCurrency.Token -> network.standardType.takeIf { it !is Network.StandardType.Unspecified }?.name
        is CryptoCurrency.Coin -> null
    }
}

private fun Network.exchangeAlias(): String? = when (toBlockchain()) {
    Blockchain.Optimism, Blockchain.OptimismTestnet -> "Optimism"
    Blockchain.Avalanche, Blockchain.AvalancheTestnet -> "AVAXC"
    else -> null
}