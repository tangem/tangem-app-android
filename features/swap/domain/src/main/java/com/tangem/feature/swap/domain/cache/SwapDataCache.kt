package com.tangem.feature.swap.domain.cache

import com.tangem.feature.swap.domain.models.ExchangeCurrencies
import com.tangem.feature.swap.domain.models.data.Currency
import com.tangem.feature.swap.domain.models.data.SwapState.QuoteModel
import java.math.BigDecimal

interface SwapDataCache {

    fun cacheNetworkId(networkId: String)
    fun cacheSwapParams(quoteModel: QuoteModel, amount: BigDecimal, fromCurrency: Currency, toCurrency: Currency)
    fun cacheAvailableToSwapTokens(networkId: String, tokens: List<Currency>)
    fun getAvailableTokens(networkId: String): List<Currency>
    fun getLastQuote(): QuoteModel?
    fun getAmountToSwap(): BigDecimal?
    fun getNetworkId(): String?
    fun getExchangeCurrencies(): ExchangeCurrencies?
}