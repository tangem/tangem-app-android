package com.tangem.tap.network.exchangeServices

import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.network.exchangeServices.mercuryo.MercuryoService
import com.tangem.tap.scope
import kotlinx.coroutines.launch

/**
[REDACTED_AUTHOR]
 * Temporary wrapper for the buy services. Service switches based on selected product type.
 */
internal class BuyExchangeService(
    private val mercuryoService: MercuryoService,
) : ExchangeService, ExchangeUrlBuilder {

    init {
        scope.launch {
            mercuryoService.update()
        }
    }

    private val currentService: ExchangeService = mercuryoService

    override suspend fun update() {
        currentService.update()
    }

    override fun featureIsSwitchedOn(): Boolean = currentService.featureIsSwitchedOn()

    override fun isBuyAllowed(): Boolean = currentService.isBuyAllowed()

    override fun isSellAllowed(): Boolean = currentService.isSellAllowed()

    override fun availableForBuy(currency: Currency): Boolean = currentService.availableForBuy(currency)

    override fun availableForSell(currency: Currency): Boolean = currentService.availableForSell(currency)

    override fun getUrl(
        action: CurrencyExchangeManager.Action,
        blockchain: Blockchain,
        cryptoCurrencyName: String,
        fiatCurrencyName: String,
        walletAddress: String,
    ): String? {
        return currentService.getUrl(action, blockchain, cryptoCurrencyName, fiatCurrencyName, walletAddress)
    }

    override fun getSellCryptoReceiptUrl(action: CurrencyExchangeManager.Action, transactionId: String): String? {
        return currentService.getSellCryptoReceiptUrl(action, transactionId)
    }
}