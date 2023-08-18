package com.tangem.data.tokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.DerivationStyleProvider
import com.tangem.domain.tokens.models.CryptoCurrency
import timber.log.Timber
import com.tangem.blockchain.common.Token as SdkToken

// FIXME: Make internal
class CryptoCurrencyFactory {

    fun createToken(
        sdkToken: SdkToken,
        blockchain: Blockchain,
        derivationStyleProvider: DerivationStyleProvider,
    ): CryptoCurrency.Token? {
        if (blockchain == Blockchain.Unknown) {
            Timber.e("Unable to map the SDK token to the domain token with Unknown blockchain")
            return null
        }

        val id = getTokenId(blockchain, sdkToken)

        return CryptoCurrency.Token(
            id = id,
            network = getNetwork(blockchain) ?: return null,
            name = sdkToken.name,
            symbol = sdkToken.symbol,
            iconUrl = getTokenIconUrl(blockchain, sdkToken),
            decimals = sdkToken.decimals,
            isCustom = isCustomToken(id),
            contractAddress = sdkToken.contractAddress,
            derivationPath = getDerivationPath(blockchain, derivationStyleProvider),
        )
    }

    fun createCoin(blockchain: Blockchain, derivationStyleProvider: DerivationStyleProvider): CryptoCurrency.Coin? {
        if (blockchain == Blockchain.Unknown) {
            Timber.e("Unable to map the SDK token to the domain token with Unknown blockchain")
            return null
        }

        return CryptoCurrency.Coin(
            id = getCoinId(blockchain),
            network = getNetwork(blockchain) ?: return null,
            name = blockchain.fullName,
            symbol = blockchain.currency,
            iconUrl = getCoinIconUrl(blockchain),
            decimals = blockchain.decimals(),
            derivationPath = getDerivationPath(blockchain, derivationStyleProvider),
        )
    }
}