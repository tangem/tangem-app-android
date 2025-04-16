package com.tangem.data.quotes.store

import androidx.datastore.core.DataStore
import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.quote.converter.QuoteConverter
import com.tangem.domain.models.StatusSource
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

internal typealias CurrencyIdWithQuote = Map<String, QuotesResponse.Quote>

/**
 * Default implementation of [QuotesStoreV2]
 *
 * @property runtimeStore         runtime store
 * @property persistenceDataStore persistence store
 * @param dispatchers             dispatchers
 */
internal class DefaultQuotesStoreV2(
    private val runtimeStore: RuntimeSharedStore<Set<Quote>>,
    private val persistenceDataStore: DataStore<CurrencyIdWithQuote>,
    dispatchers: CoroutineDispatcherProvider,
) : QuotesStoreV2 {

    private val scope = CoroutineScope(context = SupervisorJob() + dispatchers.io)

    init {
        scope.launch {
            val cachedStatuses = persistenceDataStore.data.firstOrNull()

            if (cachedStatuses.isNullOrEmpty()) return@launch

            runtimeStore.store(
                value = QuoteConverter(isCached = true).convertSet(input = cachedStatuses.entries),
            )
        }
    }

    override fun get(): Flow<Set<Quote>> = runtimeStore.get()

    override suspend fun getAllSyncOrNull(): Set<Quote>? = runtimeStore.getSyncOrNull()

    override suspend fun refresh(currenciesIds: Set<CryptoCurrency.RawID>) {
        updateStatusSourceInRuntime(currenciesIds = currenciesIds, source = StatusSource.CACHE)
    }

    override suspend fun storeActual(values: Map<String, QuotesResponse.Quote>) {
        coroutineScope {
            launch {
                val quotes = QuoteConverter(isCached = false).convertSet(input = values.entries)
                storeInRuntimeStore(values = quotes)
            }
            launch { storeInPersistenceStore(values = values) }
        }
    }

    override suspend fun storeError(currenciesIds: Set<CryptoCurrency.RawID>) {
        updateStatusSourceInRuntime(currenciesIds = currenciesIds, source = StatusSource.ONLY_CACHE)
    }

    private suspend fun updateStatusSourceInRuntime(currenciesIds: Set<CryptoCurrency.RawID>, source: StatusSource) {
        runtimeStore.update(default = emptySet()) { stored ->
            val updatedQuotes = currenciesIds.mapTo(hashSetOf()) { id ->
                val quote = stored.firstOrNull { it.rawCurrencyId == id } ?: Quote.Empty(id)

                quote.copySealed(source = source)
            }

            stored.addOrReplace(items = updatedQuotes) { old, new -> old.rawCurrencyId == new.rawCurrencyId }
        }
    }

    private suspend fun storeInRuntimeStore(values: Set<Quote>) {
        runtimeStore.update(default = emptySet()) { saved ->
            saved.addOrReplace(items = values) { prev, new -> prev.rawCurrencyId == new.rawCurrencyId }
        }
    }

    private suspend fun storeInPersistenceStore(values: Map<String, QuotesResponse.Quote>) {
        persistenceDataStore.updateData { storedQuotes -> storedQuotes + values }
    }
}