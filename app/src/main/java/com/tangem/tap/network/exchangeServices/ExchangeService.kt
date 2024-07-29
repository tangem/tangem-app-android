package com.tangem.tap.network.exchangeServices

import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.tap.common.feature.Feature
import com.tangem.tap.domain.model.Currency

interface Exchanger {
    fun isBuyAllowed(): Boolean
    fun isSellAllowed(): Boolean
    fun availableForBuy(scanResponse: ScanResponse, currency: Currency): Boolean
    fun availableForSell(currency: Currency): Boolean
}

interface ExchangeService : Feature, Exchanger, ExchangeUrlBuilder {
    suspend fun update()

    companion object {
        fun dummy(): ExchangeService = object : ExchangeService {
            override fun featureIsSwitchedOn(): Boolean = false
            override suspend fun update() {}
            override fun isBuyAllowed(): Boolean = false
            override fun isSellAllowed(): Boolean = false
            override fun availableForBuy(scanResponse: ScanResponse, currency: Currency): Boolean = false
            override fun availableForSell(currency: Currency): Boolean = false
            override fun getUrl(
                action: CurrencyExchangeManager.Action,
                cryptoCurrency: CryptoCurrency,
                fiatCurrencyName: String,
                walletAddress: String,
                isDarkTheme: Boolean,
            ): String? = null

            override fun getSellCryptoReceiptUrl(
                action: CurrencyExchangeManager.Action,
                transactionId: String,
            ): String? = null
        }
    }
}

interface ExchangeRules : Feature, Exchanger {

    companion object {
        fun dummy(): ExchangeRules = object : ExchangeRules {
            override fun featureIsSwitchedOn(): Boolean = false
            override fun isBuyAllowed(): Boolean = false
            override fun isSellAllowed(): Boolean = false
            override fun availableForBuy(scanResponse: ScanResponse, currency: Currency): Boolean = false
            override fun availableForSell(currency: Currency): Boolean = false
        }
    }
}

interface ExchangeUrlBuilder {
    @Suppress("LongParameterList")
    fun getUrl(
        action: CurrencyExchangeManager.Action,
        cryptoCurrency: CryptoCurrency,
        fiatCurrencyName: String,
        walletAddress: String,
        isDarkTheme: Boolean,
    ): String?

    fun getSellCryptoReceiptUrl(action: CurrencyExchangeManager.Action, transactionId: String): String?

    companion object {
        const val SCHEME = "https"
        const val SUCCESS_URL = "https://tangem.com/success"
    }
}
