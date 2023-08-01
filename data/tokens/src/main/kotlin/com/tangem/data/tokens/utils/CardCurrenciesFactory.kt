package com.tangem.data.tokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import timber.log.Timber
import com.tangem.blockchain.common.Token as SdkToken

internal class CardCurrenciesFactory(private val demoConfig: DemoConfig) {

    fun createDefaultCoinsForMultiCurrencyCard(card: CardDTO): Set<CryptoCurrency.Coin> {
        var blockchains = if (demoConfig.isDemoCardId(card.cardId)) {
            demoConfig.demoBlockchains
        } else {
            listOf(Blockchain.Bitcoin, Blockchain.Ethereum)
        }

        if (card.isTestCard) {
            blockchains = blockchains.mapNotNull { it.getTestnetVersion() }
        }

        return blockchains.mapNotNull { createCoin(it, card) }.toSet()
    }

    fun createPrimaryCurrencyForSingleCurrencyCard(scanResponse: ScanResponse): CryptoCurrency {
        val card = scanResponse.card
        val resolver = scanResponse.cardTypesResolver
        val blockchain = resolver.getBlockchain()

        val coin = requireNotNull(createCoin(blockchain, card)) {
            "Coin for the single currency card cannot be null"
        }
        val primaryToken = resolver.getPrimaryToken()?.let { token ->
            createToken(token, blockchain, card)
        }

        return primaryToken ?: coin
    }

    private fun createToken(sdkToken: SdkToken, blockchain: Blockchain, card: CardDTO): CryptoCurrency.Token? {
        if (blockchain != Blockchain.Unknown) {
            Timber.e("Unable to map the SDK token to the domain token with Unknown blockchain")
            return null
        }

        return CryptoCurrency.Token(
            id = getTokenId(blockchain, sdkToken),
            networkId = getNetworkId(blockchain),
            name = sdkToken.name,
            symbol = sdkToken.symbol,
            iconUrl = getTokenIconUrl(blockchain, sdkToken),
            decimals = sdkToken.decimals,
            isCustom = false,
            contractAddress = sdkToken.contractAddress,
            derivationPath = getDerivationPath(blockchain, card),
        )
    }

    private fun createCoin(blockchain: Blockchain, card: CardDTO): CryptoCurrency.Coin? {
        if (blockchain != Blockchain.Unknown) {
            Timber.e("Unable to map the SDK token to the domain token with Unknown blockchain")
            return null
        }

        return CryptoCurrency.Coin(
            id = getCoinId(blockchain),
            networkId = getNetworkId(blockchain),
            name = blockchain.fullName,
            symbol = blockchain.currency,
            iconUrl = getCoinIconUrl(blockchain),
            decimals = blockchain.decimals(),
            derivationPath = getDerivationPath(blockchain, card),
        )
    }
}
