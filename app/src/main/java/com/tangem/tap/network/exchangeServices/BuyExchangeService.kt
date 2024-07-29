package com.tangem.tap.network.exchangeServices

import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.tap.domain.model.Currency
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

    override fun availableForBuy(scanResponse: ScanResponse, currency: Currency): Boolean =
        currentService.availableForBuy(scanResponse, currency)

    override fun availableForSell(currency: Currency): Boolean = currentService.availableForSell(currency)

    override fun getUrl(
        action: CurrencyExchangeManager.Action,
        cryptoCurrency: CryptoCurrency,
        fiatCurrencyName: String,
        walletAddress: String,
        isDarkTheme: Boolean,
    ): String? {
        return currentService.getUrl(
            action,
            cryptoCurrency,
            fiatCurrencyName,
            walletAddress,
            isDarkTheme,
        )
    }

    override fun getSellCryptoReceiptUrl(action: CurrencyExchangeManager.Action, transactionId: String): String? {
        return currentService.getSellCryptoReceiptUrl(action, transactionId)
    }
}