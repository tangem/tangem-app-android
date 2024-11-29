package com.tangem.tap.network.exchangeServices

import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.tap.domain.model.Currency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

typealias ExchangeServiceInitializationStatus = Lce<Throwable, Any>

interface Exchanger {
    fun isBuyAllowed(): Boolean
    fun isSellAllowed(): Boolean
    fun availableForBuy(scanResponse: ScanResponse, currency: Currency): Boolean
    fun availableForSell(currency: Currency): Boolean
}

interface ExchangeService : Exchanger, ExchangeUrlBuilder {

    val initializationStatus: StateFlow<ExchangeServiceInitializationStatus>

    suspend fun update()

    companion object {
        fun dummy(): ExchangeService = object : ExchangeService {

            override val initializationStatus: StateFlow<ExchangeServiceInitializationStatus> =
                MutableStateFlow(value = lceLoading())

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

interface ExchangeRules : Exchanger {

    companion object {
        fun dummy(): ExchangeRules = object : ExchangeRules {
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