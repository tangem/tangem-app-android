package com.tangem.tap.network.exchangeServices

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.moonpay.MoonPaySellRepository
import kotlinx.coroutines.flow.StateFlow

internal class MoonPaySellServiceAdapter(
    private val repository: MoonPaySellRepository,
) : SellService {

    override val initializationStatus: StateFlow<SellServiceInitializationStatus>
        get() = repository.initializationStatus

    override suspend fun update() = repository.update()

    override fun availableForSell(currency: CryptoCurrency): Boolean = repository.availableForSell(currency)

    override fun getUrl(
        cryptoCurrency: CryptoCurrency,
        fiatCurrencyName: String,
        walletAddress: String,
        isDarkTheme: Boolean,
        requestId: String,
    ): String? {
        return repository.getUrl(
            cryptoCurrency = cryptoCurrency,
            fiatCurrencyName = fiatCurrencyName,
            walletAddress = walletAddress,
            isDarkTheme = isDarkTheme,
            requestId = requestId,
        )
    }
}