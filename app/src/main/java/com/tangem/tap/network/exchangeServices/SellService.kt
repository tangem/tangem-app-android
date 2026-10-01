package com.tangem.tap.network.exchangeServices

import com.tangem.domain.core.lce.Lce
import com.tangem.domain.models.currency.CryptoCurrency
import kotlinx.coroutines.flow.StateFlow

typealias SellServiceInitializationStatus = Lce<Throwable, Any>

interface SellService {

    val initializationStatus: StateFlow<SellServiceInitializationStatus>

    suspend fun update()

    fun availableForSell(currency: CryptoCurrency): Boolean

    fun getUrl(
        cryptoCurrency: CryptoCurrency,
        fiatCurrencyName: String,
        walletAddress: String,
        isDarkTheme: Boolean,
        requestId: String,
    ): String?
}