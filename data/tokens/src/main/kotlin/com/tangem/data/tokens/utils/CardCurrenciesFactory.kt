package com.tangem.data.tokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.DerivationStyleProvider
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.models.CryptoCurrency

internal class CardCurrenciesFactory(private val demoConfig: DemoConfig) {

    private val cryptoCurrencyFactory by lazy { CryptoCurrencyFactory() }

    fun createDefaultCoinsForMultiCurrencyCard(
        card: CardDTO,
        derivationStyleProvider: DerivationStyleProvider,
    ): List<CryptoCurrency.Coin> {
        var blockchains = if (demoConfig.isDemoCardId(card.cardId)) {
            demoConfig.demoBlockchains
        } else {
            listOf(Blockchain.Bitcoin, Blockchain.Ethereum)
        }

        if (card.isTestCard) {
            blockchains = blockchains.mapNotNull { it.getTestnetVersion() }
        }

        return blockchains.mapNotNull { cryptoCurrencyFactory.createCoin(it, derivationStyleProvider) }
    }

    fun createPrimaryCurrencyForSingleCurrencyCard(scanResponse: ScanResponse): CryptoCurrency {
        val derivationStyleProvider = scanResponse.derivationStyleProvider
        val resolver = scanResponse.cardTypesResolver
        val blockchain = resolver.getBlockchain()

        val coin = requireNotNull(cryptoCurrencyFactory.createCoin(blockchain, derivationStyleProvider)) {
            "Coin for the single currency card cannot be null"
        }
        val primaryToken = resolver.getPrimaryToken()?.let { token ->
            cryptoCurrencyFactory.createToken(token, blockchain, derivationStyleProvider)
        }

        return primaryToken ?: coin
    }
}
