package com.tangem.domain.quotes

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import java.math.BigDecimal

/**
 * Get currency USD quote use case
 */
class GetCurrencyUSDQuoteUseCase(
    private val quotesRepository: QuotesRepository,
) {

    /** Get quote by [currencyId] synchronously or null */
    suspend operator fun invoke(currencyId: CryptoCurrency.RawID): BigDecimal? {
        val value = quotesRepository.getCurrencyUSDQuote(currencyId)?.value

        return if (value is QuoteStatus.Data) {
            value.fiatRateUSD
        } else {
            null
        }
    }
}