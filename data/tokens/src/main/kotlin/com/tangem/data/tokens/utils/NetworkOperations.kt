package com.tangem.data.tokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.tokens.models.Network
import timber.log.Timber

internal fun getNetwork(blockchain: Blockchain): Network? {
    if (blockchain == Blockchain.Unknown) {
        Timber.e("Unable to convert Unknown blockchain to the domain network model")
        return null
    }

    return Network(
        id = Network.ID(blockchain.id),
        name = blockchain.fullName,
        isTestnet = blockchain.isTestnet(),
        standardType = getNetworkStandardType(blockchain),
    )
}

private fun getNetworkStandardType(blockchain: Blockchain): Network.StandardType {
    return when (blockchain) {
        Blockchain.Ethereum, Blockchain.EthereumTestnet -> Network.StandardType.ERC20
        Blockchain.BSC, Blockchain.BSCTestnet -> Network.StandardType.BEP20
        Blockchain.Binance, Blockchain.BinanceTestnet -> Network.StandardType.BEP2
        Blockchain.Tron, Blockchain.TronTestnet -> Network.StandardType.TRC20
        else -> Network.StandardType.Unspecified(blockchain.name)
    }
}