package com.tangem.tap.network.exchangeServices

import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.features.wallet.models.Currency

interface ExchangeService {
    suspend fun update()
    fun isBuyAllowed(): Boolean
    fun isSellAllowed(): Boolean
    fun availableForBuy(currency: Currency):Boolean
    fun availableForSell(currency: Currency):Boolean
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