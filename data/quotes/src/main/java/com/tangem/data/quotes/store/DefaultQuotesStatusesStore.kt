package com.tangem.data.quotes.store

import androidx.datastore.core.DataStore
import com.tangem.data.quotes.converter.FiatCurrencyConverter
import com.tangem.data.quotes.converter.QuoteStatusConverter
import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.FiatCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.extensions.addOrReplace
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File

internal typealias CurrencyIdWithQuote = Map<String, QuotesResponse.Quote>

/**
 * Default implementation of [QuotesStatusesStore]
 *
 * @property runtimeStore         runtime store
 * @property persistenceDataStore persistence store (keeps quotes together with the fiat currency they're expressed in)
 * @property legacyCacheFile      pre-v2 cache file kept on disk; deleted once on init
 * @param scope                   app coroutine scope
 */
internal class DefaultQuotesStatusesStore(
    private val runtimeStore: RuntimeSharedStore<Set<QuoteStatus>>,
    private val persistenceDataStore: DataStore<QuoteStatusDM>,
    private val legacyCacheFile: File,
    private val scope: AppCoroutineScope,
) : QuotesStatusesStore {

    init {
        scope.launch {
            deleteLegacyCacheFile()

            val cached = persistenceDataStore.data.firstOrNull() ?: return@launch
            val fiatCurrency = cached.fiatCurrency?.let(FiatCurrencyConverter::convertBack) ?: return@launch

            if (cached.quotes.isEmpty()) return@launch

            runtimeStore.store(
                value = QuoteStatusConverter(source = StatusSource.CACHE, fiatCurrency = fiatCurrency)
                    .convertSet(input = cached.quotes.entries),
            )
        }
    }

    private fun deleteLegacyCacheFile() {
        if (!legacyCacheFile.exists()) return
        runCatching { legacyCacheFile.delete() }
            .onSuccess { deleted ->
                if (deleted) {
                    TangemLogger.i("Deleted legacy quotes cache file: ${legacyCacheFile.name}")
                } else {
                    TangemLogger.e("Could not delete legacy quotes cache file: ${legacyCacheFile.name}")
                }
            }
            .onFailure { TangemLogger.e("Failed to delete legacy quotes cache file", it) }
    }

    override fun get(): Flow<Set<QuoteStatus>> = runtimeStore.get()

    override suspend fun getAllSyncOrNull(): Set<QuoteStatus>? = runtimeStore.getSyncOrNull()

    override suspend fun updateStatusSource(
        currencyId: CryptoCurrency.RawID,
        source: StatusSource,
        ifNotFound: (CryptoCurrency.RawID) -> QuoteStatus?,
    ) {
        updateStatusSource(
            currenciesIds = setOf(currencyId),
            source = source,
            ifNotFound = ifNotFound,
        )
    }

    override suspend fun updateStatusSource(
        currenciesIds: Set<CryptoCurrency.RawID>,
        source: StatusSource,
        ifNotFound: (CryptoCurrency.RawID) -> QuoteStatus?,
    ) {
        if (currenciesIds.isEmpty()) {
            TangemLogger.d("Nothing to update: currencies ids are empty")
            return
        }

        runtimeStore.update(default = emptySet()) { stored ->
            val updatedQuotes = currenciesIds.mapNotNullTo(hashSetOf()) { id ->
                val quote = stored.firstOrNull { it.rawCurrencyId == id }
                    ?: ifNotFound(id)
                    ?: return@mapNotNullTo null

                quote.copy(value = quote.value.copySealed(source = source))
            }

            stored.addOrReplace(items = updatedQuotes) { old, new -> old.rawCurrencyId == new.rawCurrencyId }
        }
    }

    override suspend fun store(values: CurrencyIdWithQuote, fiatCurrency: FiatCurrency) {
        if (values.isEmpty()) return

        coroutineScope {
            launch { storeInRuntime(values = values, fiatCurrency = fiatCurrency) }
            launch { storeInPersistence(values = values, fiatCurrency = fiatCurrency) }
        }
    }

    private suspend fun storeInRuntime(values: CurrencyIdWithQuote, fiatCurrency: FiatCurrency) {
        val quotes = QuoteStatusConverter(source = StatusSource.ACTUAL, fiatCurrency = fiatCurrency)
            .convertSet(input = values.entries)

        runtimeStore.update(default = emptySet()) { saved ->
            saved.addOrReplace(items = quotes) { prev, new -> prev.rawCurrencyId == new.rawCurrencyId }
        }
    }

    private suspend fun storeInPersistence(values: CurrencyIdWithQuote, fiatCurrency: FiatCurrency) {
        persistenceDataStore.updateData { stored ->
            val isSameCurrency = stored.fiatCurrency?.code == fiatCurrency.code
            val mergedQuotes = if (isSameCurrency) stored.quotes + values else values

            QuoteStatusDM(
                fiatCurrency = FiatCurrencyConverter.convert(fiatCurrency),
                quotes = mergedQuotes,
            )
        }
    }
}