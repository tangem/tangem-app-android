package com.tangem.data.tokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.DerivationStyleProvider
import com.tangem.domain.common.extensions.toCoinId
import com.tangem.domain.tokens.models.CryptoCurrency
import timber.log.Timber
import com.tangem.blockchain.common.Token as SdkToken

// FIXME: Make internal
class CryptoCurrencyFactory {

    fun createToken(
        sdkToken: SdkToken,
        blockchain: Blockchain,
        extraDerivationPath: String?,
        derivationStyleProvider: DerivationStyleProvider,
    ): CryptoCurrency.Token? {
        if (blockchain == Blockchain.Unknown) {
            Timber.e("Unable to map the SDK token to the domain token with Unknown blockchain")
            return null
        }

        val network = getNetwork(blockchain, extraDerivationPath, derivationStyleProvider) ?: return null
        val id = getTokenId(network, sdkToken)

        return CryptoCurrency.Token(
            id = id,
            network = network,
            name = sdkToken.name,
            symbol = sdkToken.symbol,
            iconUrl = getTokenIconUrl(blockchain, sdkToken),
            decimals = sdkToken.decimals,
            isCustom = isCustomToken(id, network),
            contractAddress = sdkToken.contractAddress,
        )
    }

    fun createCoin(
        blockchain: Blockchain,
        extraDerivationPath: String?,
        derivationStyleProvider: DerivationStyleProvider,
    ): CryptoCurrency.Coin? {
        if (blockchain == Blockchain.Unknown) {
            Timber.e("Unable to map the SDK token to the domain token with Unknown blockchain")
            return null
        }
        val network = getNetwork(blockchain, extraDerivationPath, derivationStyleProvider) ?: return null

        return CryptoCurrency.Coin(
            id = getCoinId(network, blockchain.toCoinId()),
            network = network,
            name = blockchain.fullName,
            symbol = blockchain.currency,
            iconUrl = getCoinIconUrl(blockchain),
            decimals = blockchain.decimals(),
            isCustom = isCustomCoin(network),
        )
    }
}