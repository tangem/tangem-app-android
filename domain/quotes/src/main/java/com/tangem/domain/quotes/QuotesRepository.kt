package com.tangem.domain.quotes

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import java.math.BigDecimal

/**
 * Quotes repository
 *
[REDACTED_AUTHOR]
 */
interface QuotesRepository {

    /** Get quotes by [currenciesIds] synchronously or null */
    suspend fun getMultiQuoteSyncOrNull(currenciesIds: Set<CryptoCurrency.RawID>): Set<QuoteStatus>?

    /** Get quote by [currencyId] synchronously or null */
    suspend fun getCurrencyUSDQuote(currencyId: CryptoCurrency.RawID): QuoteStatus?

    /** USD value of one unit of the fiat currency [fiatCurrencyCode] (e.g. "EUR"), or null if it can't be fetched */
    suspend fun getFiatUSDRate(fiatCurrencyCode: String): BigDecimal?
}