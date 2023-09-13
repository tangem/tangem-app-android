package com.tangem.data.tokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.DerivationStyleProvider
import com.tangem.domain.tokens.models.Network
import timber.log.Timber

internal fun getBlockchain(networkId: Network.ID): Blockchain {
    return Blockchain.fromId(networkId.value)
}

internal fun getNetwork(
    blockchain: Blockchain,
    extraDerivationPath: String?,
    derivationStyleProvider: DerivationStyleProvider,
): Network? {
    if (blockchain == Blockchain.Unknown) {
        Timber.e("Unable to convert Unknown blockchain to the domain network model")
        return null
    }

    return Network(
        id = Network.ID(blockchain.id),
        name = blockchain.fullName,
        isTestnet = blockchain.isTestnet(),
        derivationPath = getDerivationPath(blockchain, extraDerivationPath, derivationStyleProvider),
        standardType = getNetworkStandardType(blockchain),
    )
}

private fun getDerivationPath(
    blockchain: Blockchain,
    extraDerivationPath: String?,
    derivationStyleProvider: DerivationStyleProvider,
): Network.DerivationPath {
    val cardDerivationPath = getCardDerivationPath(blockchain, derivationStyleProvider)

    return when {
        cardDerivationPath.isNullOrBlank() -> Network.DerivationPath.None
        extraDerivationPath == cardDerivationPath -> Network.DerivationPath.Card(extraDerivationPath)
        !extraDerivationPath.isNullOrBlank() -> Network.DerivationPath.Custom(extraDerivationPath)
        else -> Network.DerivationPath.None
    }
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

private fun getCardDerivationPath(blockchain: Blockchain, derivationStyleProvider: DerivationStyleProvider): String? {
    return blockchain.derivationPath(derivationStyleProvider.getDerivationStyle())?.rawPath
}