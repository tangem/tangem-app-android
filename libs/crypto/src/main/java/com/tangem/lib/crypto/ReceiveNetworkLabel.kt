package com.tangem.lib.crypto

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network

private const val OPTIMISM_LABEL = "Optimism"
private const val AVALANCHE_C_CHAIN_LABEL = "AVAXC"

fun CryptoCurrency.receiveNetworkLabel(): String? {
    network.exchangeAlias()?.let { return it }

    return when (this) {
        is CryptoCurrency.Token -> network.standardType.takeIf { it !is Network.StandardType.Unspecified }?.name
        is CryptoCurrency.Coin -> null
    }
}

private fun Network.exchangeAlias(): String? = when (toBlockchain()) {
    Blockchain.Optimism, Blockchain.OptimismTestnet -> OPTIMISM_LABEL
    Blockchain.Avalanche, Blockchain.AvalancheTestnet -> AVALANCHE_C_CHAIN_LABEL
    else -> null
}