package com.tangem.domain.moonpay

import com.tangem.domain.core.lce.Lce
import com.tangem.domain.models.currency.CryptoCurrency
import kotlinx.coroutines.flow.StateFlow

typealias MoonPaySellInitializationStatus = Lce<Throwable, Any>

/**
 * [availableForSell], and [getUrl] for a mainnet currency, rely on the sell listing fetched by
 * [update] — until [update] has completed at least once, they return `false`/`null` rather than
 * throwing or suspending. A testnet currency's [getUrl] does not need [update]: it returns a
 * blockchain-specific testnet top-up URL without consulting the listing.
 */
interface MoonPaySellRepository {

    val initializationStatus: StateFlow<MoonPaySellInitializationStatus>

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