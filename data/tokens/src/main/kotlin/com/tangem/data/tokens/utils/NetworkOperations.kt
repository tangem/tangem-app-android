package com.tangem.data.tokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.FeePaidCurrency
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.common.DerivationStyleProvider
import com.tangem.domain.tokens.model.Network
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
        backendId = blockchain.toNetworkId(),
        name = blockchain.getNetworkName(),
        isTestnet = blockchain.isTestnet(),
        derivationPath = getNetworkDerivationPath(blockchain, extraDerivationPath, derivationStyleProvider),
        currencySymbol = blockchain.currency,
        standardType = getNetworkStandardType(blockchain),
        hasFiatFeeRate = blockchain.feePaidCurrency() !is FeePaidCurrency.FeeResource,
    )
}

private fun getNetworkDerivationPath(
    blockchain: Blockchain,
    extraDerivationPath: String?,
    cardDerivationStyleProvider: DerivationStyleProvider,
): Network.DerivationPath {
    val defaultDerivationPath = getDefaultDerivationPath(blockchain, cardDerivationStyleProvider)

    return if (extraDerivationPath.isNullOrBlank()) {
        if (defaultDerivationPath.isNullOrBlank()) {
            Network.DerivationPath.None
        } else {
            Network.DerivationPath.Card(defaultDerivationPath)
        }
    } else {
        if (extraDerivationPath == defaultDerivationPath) {
            Network.DerivationPath.Card(defaultDerivationPath)
        } else {
            Network.DerivationPath.Custom(extraDerivationPath)
        }
    }
}

internal fun getNetworkStandardType(blockchain: Blockchain): Network.StandardType {
    return when (blockchain) {
        Blockchain.Ethereum, Blockchain.EthereumTestnet -> Network.StandardType.ERC20
        Blockchain.BSC, Blockchain.BSCTestnet -> Network.StandardType.BEP20
        Blockchain.Binance, Blockchain.BinanceTestnet -> Network.StandardType.BEP2
        Blockchain.Tron, Blockchain.TronTestnet -> Network.StandardType.TRC20
        else -> Network.StandardType.Unspecified(blockchain.name)
    }
}

private fun getDefaultDerivationPath(
    blockchain: Blockchain,
    derivationStyleProvider: DerivationStyleProvider,
): String? {
    return blockchain.derivationPath(derivationStyleProvider.getDerivationStyle())?.rawPath
}
