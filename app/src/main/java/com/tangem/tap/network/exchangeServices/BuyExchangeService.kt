package com.tangem.tap.network.exchangeServices

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.models.scan.ProductType
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.network.exchangeServices.mercuryo.MercuryoService
import com.tangem.tap.network.exchangeServices.utorg.UtorgExchangeService
import com.tangem.tap.scope
import kotlinx.coroutines.launch

/**
[REDACTED_AUTHOR]
 * Temporary wrapper for the buy services. Service switches based on selected product type.
 * Just now - UtorgService used only for the SaltPay cards
 */
internal class BuyExchangeService(
    private val productTypeProvider: () -> ProductType?,
    private val mercuryoService: MercuryoService,
    private val utorgService: UtorgExchangeService,
) : ExchangeService, ExchangeUrlBuilder {

    init {
        scope.launch {
            mercuryoService.update()
            utorgService.update()
        }
    }

    private val currentService: ExchangeService
        get() = when (productTypeProvider.invoke()) {
            ProductType.SaltPay -> utorgService
            else -> mercuryoService
        }

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