package com.tangem.tap.network.exchangeServices

import com.tangem.domain.core.lce.Lce
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.tap.domain.model.Currency
import kotlinx.coroutines.flow.StateFlow

typealias SellServiceInitializationStatus = Lce<Throwable, Any>

interface SellService {

    val initializationStatus: StateFlow<SellServiceInitializationStatus>

    suspend fun update()

    fun availableForSell(currency: Currency): Boolean

    fun getUrl(
        cryptoCurrency: CryptoCurrency,
        fiatCurrencyName: String,
        walletAddress: String,
        isDarkTheme: Boolean,
    ): String?

    fun getSellCryptoReceiptUrl(transactionId: String): String?
}