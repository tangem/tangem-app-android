package com.tangem.features.earn.impl.analytics

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.quotes.GetCurrencyUSDQuoteUseCase
import java.math.BigDecimal
import javax.inject.Inject

internal class EarnUsdValuation @Inject constructor(
    private val getCurrencyUSDQuote: GetCurrencyUSDQuoteUseCase,
) {

    suspend fun bucket(currency: CryptoCurrency, cryptoAmount: BigDecimal?): String? {
        val amount = cryptoAmount ?: return null
        val rawCurrencyId = currency.id.rawCurrencyId ?: return null
        val usdQuote = getCurrencyUSDQuote(rawCurrencyId) ?: return null

        return usdBucketOf(amount.multiply(usdQuote))
    }
}