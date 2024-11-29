package com.tangem.data.common.currency

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.FeePaidCurrency
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.common.DerivationStyleProvider
import com.tangem.domain.common.extensions.canHandleToken
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.Network
import timber.log.Timber

fun getBlockchain(networkId: Network.ID): Blockchain {
    return Blockchain.fromId(networkId.value)
}

fun getNetwork(
    blockchain: Blockchain,
    extraDerivationPath: String?,
    derivationStyleProvider: DerivationStyleProvider?,
    excludedBlockchains: ExcludedBlockchains,
    canHandleTokens: Boolean,
): Network? {
    if (!isBlockchainSupported(blockchain, excludedBlockchains)) {
        return null
    }

    return Network(
        id = Network.ID(blockchain.id),
        backendId = blockchain.toNetworkId(),
        name = blockchain.getNetworkName(),
        isTestnet = blockchain.isTestnet(),
        derivationPath = getNetworkDerivationPath(
            blockchain = blockchain,
            extraDerivationPath = extraDerivationPath,
            cardDerivationStyleProvider = derivationStyleProvider,
        ),
        currencySymbol = blockchain.currency,
        standardType = getNetworkStandardType(blockchain),
        hasFiatFeeRate = blockchain.feePaidCurrency() !is FeePaidCurrency.FeeResource,
        canHandleTokens = canHandleTokens,
    )
}

fun getNetwork(
    networkId: Network.ID,
    derivationPath: Network.DerivationPath,
    scanResponse: ScanResponse,
    excludedBlockchains: ExcludedBlockchains,
): Network? {
    val blockchain = getBlockchain(networkId)
    if (!isBlockchainSupported(blockchain, excludedBlockchains)) {
        return null
    }

    return Network(
        id = networkId,
        backendId = blockchain.toNetworkId(),
        name = blockchain.getNetworkName(),
        isTestnet = blockchain.isTestnet(),
        derivationPath = derivationPath,
        currencySymbol = blockchain.currency,
        standardType = getNetworkStandardType(blockchain),
        hasFiatFeeRate = blockchain.feePaidCurrency() !is FeePaidCurrency.FeeResource,
        canHandleTokens = scanResponse.card.canHandleToken(
            blockchain,
            scanResponse.cardTypesResolver,
            excludedBlockchains,
        ),
    )
}

private fun isBlockchainSupported(blockchain: Blockchain, excludedBlockchains: ExcludedBlockchains): Boolean {
    if (blockchain == Blockchain.Unknown) {
        Timber.w("Unable to convert Unknown blockchain to the domain network model")
        return false
    }
    if (blockchain in excludedBlockchains) {
        Timber.w("Unable to convert excluded blockchain to the domain network model")
        return false
    }

    return true
}

fun getNetwork(
    blockchain: Blockchain,
    extraDerivationPath: String?,
    scanResponse: ScanResponse,
    excludedBlockchains: ExcludedBlockchains,
): Network? {
    return getNetwork(
        blockchain = blockchain,
        extraDerivationPath = extraDerivationPath,
        derivationStyleProvider = scanResponse.derivationStyleProvider,
        excludedBlockchains = excludedBlockchains,
        canHandleTokens = scanResponse.card.canHandleToken(
            blockchain,
            scanResponse.cardTypesResolver,
            excludedBlockchains,
        ),
    )
}

fun getNetworkDerivationPath(
    blockchain: Blockchain,
    extraDerivationPath: String?,
    cardDerivationStyleProvider: DerivationStyleProvider?,
): Network.DerivationPath {
    if (cardDerivationStyleProvider == null) {
        return Network.DerivationPath.None
    }

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

fun getNetworkStandardType(blockchain: Blockchain): Network.StandardType {
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