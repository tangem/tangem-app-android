package com.tangem.data.tokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency

class CardCryptoCurrenciesFactory(
    private val demoConfig: DemoConfig,
    excludedBlockchains: ExcludedBlockchains,
) {

    private val cryptoCurrencyFactory = CryptoCurrencyFactory(excludedBlockchains)

    fun createDefaultCoinsForMultiCurrencyCard(scanResponse: ScanResponse): List<CryptoCurrency.Coin> {
        val card = scanResponse.card

        var blockchains = if (demoConfig.isDemoCardId(card.cardId)) {
            demoConfig.demoBlockchains
        } else {
            listOf(Blockchain.Bitcoin, Blockchain.Ethereum)
        }

        if (card.isTestCard) {
            blockchains = blockchains.mapNotNull { it.getTestnetVersion() }
        }

        return blockchains.mapNotNull {
            cryptoCurrencyFactory.createCoin(
                blockchain = it,
                extraDerivationPath = null,
                scanResponse = scanResponse,
            )
        }
    }

    fun createPrimaryCurrencyForSingleCurrencyCard(scanResponse: ScanResponse): CryptoCurrency {
        val resolver = scanResponse.cardTypesResolver
        val blockchain = resolver.getBlockchain()

        val coin = cryptoCurrencyFactory.createCoin(
            blockchain = blockchain,
            extraDerivationPath = null,
            scanResponse = scanResponse,
        )
        requireNotNull(coin) { "Coin for the single currency card cannot be null" }

        val primaryToken = resolver.getPrimaryToken()?.let { token ->
            cryptoCurrencyFactory.createToken(
                sdkToken = token,
                blockchain = blockchain,
                extraDerivationPath = null,
                scanResponse = scanResponse,
            )
        }

        return primaryToken ?: coin
    }

    fun createCurrenciesForSingleCurrencyCardWithToken(scanResponse: ScanResponse): List<CryptoCurrency> {
        val resolver = scanResponse.cardTypesResolver
        val blockchain = resolver.getBlockchain()

        val coin = cryptoCurrencyFactory.createCoin(
            blockchain = blockchain,
            extraDerivationPath = null,
            scanResponse = scanResponse,
        )
        requireNotNull(coin) { "Coin for the single currency card cannot be null" }

        val primaryToken = resolver.getPrimaryToken()?.let { token ->
            cryptoCurrencyFactory.createToken(
                sdkToken = token,
                blockchain = blockchain,
                extraDerivationPath = null,
                scanResponse = scanResponse,
            )
        }

        return listOfNotNull(coin, primaryToken)
    }
}