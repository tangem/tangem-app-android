package com.tangem.tap.network.exchangeServices

import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.domain.model.Currency

/**
[REDACTED_AUTHOR]
 */
class CurrencyExchangeManager(
    private val buyService: ExchangeService,
    private val sellService: ExchangeService,
    private val primaryRules: ExchangeRules,
) : ExchangeService {

    override fun featureIsSwitchedOn(): Boolean = primaryRules.featureIsSwitchedOn()

    override suspend fun update() {
        buyService.update()
        sellService.update()
    }

    override fun isBuyAllowed(): Boolean = primaryRules.isBuyAllowed() && buyService.isBuyAllowed()
    override fun isSellAllowed(): Boolean = primaryRules.isSellAllowed() && sellService.isSellAllowed()

    override fun availableForBuy(currency: Currency): Boolean {
        return primaryRules.availableForBuy(currency) && buyService.availableForBuy(currency)
    }

    override fun availableForSell(currency: Currency): Boolean {
        return primaryRules.availableForSell(currency) && sellService.availableForSell(currency)
    }

    override fun getUrl(
        action: Action,
        blockchain: Blockchain,
        cryptoCurrencyName: CryptoCurrencyName,
        fiatCurrencyName: String,
        walletAddress: String,
        isDarkTheme: Boolean,
    ): String? {
        if (blockchain.isTestnet()) return blockchain.getTestnetTopUpUrl()

        val urlBuilder = getExchangeUrlBuilder(action)
        return urlBuilder.getUrl(
            action,
            blockchain,
            cryptoCurrencyName,
            fiatCurrencyName,
            walletAddress,
            isDarkTheme,
        )
    }

    override fun getSellCryptoReceiptUrl(action: Action, transactionId: String): String? {
        val urlBuilder = getExchangeUrlBuilder(action)
        return urlBuilder.getSellCryptoReceiptUrl(action, transactionId)
    }

    private fun getExchangeUrlBuilder(action: Action): ExchangeUrlBuilder {
        return when (action) {
            Action.Buy -> buyService
            Action.Sell -> sellService
        }
    }

    enum class Action { Buy, Sell }

    companion object {
        fun dummy(): CurrencyExchangeManager = CurrencyExchangeManager(
            buyService = ExchangeService.dummy(),
            sellService = ExchangeService.dummy(),
            primaryRules = ExchangeRules.dummy(),
        )
    }
}