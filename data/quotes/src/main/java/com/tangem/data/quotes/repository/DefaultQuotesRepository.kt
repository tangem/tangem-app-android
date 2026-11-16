package com.tangem.data.quotes.repository

import arrow.core.getOrElse
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.data.common.quote.QuotesFetcher
import com.tangem.data.quotes.store.QuotesStatusesStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.utils.logging.TangemLogger
import java.math.BigDecimal
import java.math.MathContext

/**
 * Default implementation of [QuotesRepository]
 *
 * @property quotesStatusesStore quotes statuses store
 * @property quotesFetcher       quotes fetcher
 *
[REDACTED_AUTHOR]
 */
internal class DefaultQuotesRepository(
    private val quotesStatusesStore: QuotesStatusesStore,
    private val quotesFetcher: QuotesFetcher,
) : QuotesRepository {

    override suspend fun getMultiQuoteSyncOrNull(currenciesIds: Set<CryptoCurrency.RawID>): Set<QuoteStatus> {
        if (currenciesIds.isEmpty()) {
            TangemLogger.e("currenciesIds are empty")
            return emptySet()
        }

        val storedQuotes = quotesStatusesStore.getAllSyncOrNull()
            ?.filter { it.rawCurrencyId in currenciesIds }

        return currenciesIds.mapTo(hashSetOf()) { currencyId ->
            storedQuotes?.firstOrNull { it.rawCurrencyId == currencyId }
                ?: QuoteStatus(rawCurrencyId = currencyId)
        }
    }

    override suspend fun getCurrencyUSDQuote(currencyId: CryptoCurrency.RawID): QuoteStatus? {
        return getMultiQuoteSyncOrNull(currenciesIds = setOf(currencyId)).firstOrNull()
    }

    /**
     * The quotes API has no fiat-to-fiat rate, so it is derived from a coin priced in both currencies:
     * (coin price in USD) / (coin price in [fiatCurrencyCode]). Any coin gives the same ratio; Bitcoin is used
     * because it is the most widely quoted one.
     *
     * [QuotesFetcher.Field.ALL_PRICES] is requested although only two prices are read: the fetcher caches quotes per
     * fiat and coin regardless of fields, so a partial quote would be served to wallet refreshes as a 0% price change.
     */
    override suspend fun getFiatUSDRate(fiatCurrencyCode: String): BigDecimal? {
        val response = quotesFetcher.fetch(
            fiatCurrencyId = fiatCurrencyCode.uppercase(),
            currenciesIds = setOf(pivotCoinId),
            fields = setOf(QuotesFetcher.Field.ALL_PRICES),
        ).getOrElse { error ->
            TangemLogger.e("Unable to fetch $fiatCurrencyCode to USD rate: $error")
            return null
        }

        val quote = response.quotes[pivotCoinId]
        val price = quote?.price?.takeIf { it.signum() > 0 }
        val priceUsd = quote?.priceUsd
        if (price == null || priceUsd == null) {
            TangemLogger.e(
                "Unable to derive $fiatCurrencyCode to USD rate from $pivotCoinId quote: " +
                    "price=${quote?.price}, priceUsd=${quote?.priceUsd}",
            )
            return null
        }

        return priceUsd.divide(price, MathContext.DECIMAL64)
    }

    private companion object {
        val pivotCoinId = Blockchain.Bitcoin.toCoinId()
    }
}