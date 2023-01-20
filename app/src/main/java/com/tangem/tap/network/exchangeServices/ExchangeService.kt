package com.tangem.tap.network.exchangeServices

import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.common.feature.Feature
import com.tangem.tap.features.wallet.models.Currency

interface Exchanger {
    fun isBuyAllowed(): Boolean
    fun isSellAllowed(): Boolean
    fun availableForBuy(currency: Currency): Boolean
    fun availableForSell(currency: Currency): Boolean
}

interface ExchangeService : Feature, Exchanger {
    suspend fun update()

    companion object {
        fun dummy(): ExchangeService = object : ExchangeService {
            override fun featureIsSwitchedOn(): Boolean = false
            override suspend fun update() {}
            override fun isBuyAllowed(): Boolean = false
            override fun isSellAllowed(): Boolean = false
            override fun availableForBuy(currency: Currency): Boolean = false
            override fun availableForSell(currency: Currency): Boolean = false
        }
    }
}

interface ExchangeRules : Feature, Exchanger {

    companion object {
        fun dummy(): ExchangeRules = object : ExchangeRules {
            override fun featureIsSwitchedOn(): Boolean = false
            override fun isBuyAllowed(): Boolean = false
            override fun isSellAllowed(): Boolean = false
            override fun availableForBuy(currency: Currency): Boolean = false
            override fun availableForSell(currency: Currency): Boolean = false
        }
    }
}

interface ExchangeUrlBuilder {
    fun getUrl(
        action: CurrencyExchangeManager.Action,
        blockchain: Blockchain,
        cryptoCurrencyName: String,
        fiatCurrencyName: String,
        walletAddress: String,
    ): String?

    fun getSellCryptoReceiptUrl(action: CurrencyExchangeManager.Action, transactionId: String): String?

    companion object {
        const val SCHEME = "https"
        const val SUCCESS_URL = "tangem://success.tangem.com"
    }
}
