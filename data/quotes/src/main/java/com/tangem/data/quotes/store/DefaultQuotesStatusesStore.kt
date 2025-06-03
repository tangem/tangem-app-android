package com.tangem.data.quotes.store

import androidx.datastore.core.DataStore
import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.quote.converter.QuoteStatusConverter
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.QuoteStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber

internal typealias CurrencyIdWithQuote = Map<String, QuotesResponse.Quote>

/**
 * Default implementation of [QuotesStatusesStore]
 *
 * @property runtimeStore         runtime store
 * @property persistenceDataStore persistence store
 * @param dispatchers             dispatchers
 */
internal class DefaultQuotesStatusesStore(
    private val runtimeStore: RuntimeSharedStore<Set<QuoteStatus>>,
    private val persistenceDataStore: DataStore<CurrencyIdWithQuote>,
    dispatchers: CoroutineDispatcherProvider,
) : QuotesStatusesStore {

    private val scope = CoroutineScope(context = SupervisorJob() + dispatchers.io)

    init {
        scope.launch {
            val cachedStatuses = persistenceDataStore.data.firstOrNull()

            if (cachedStatuses.isNullOrEmpty()) return@launch

            runtimeStore.store(
                value = QuoteStatusConverter(isCached = true).convertSet(input = cachedStatuses.entries),
            )
        }
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
            Timber.d("Nothing to update: currencies ids are empty")
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

    override suspend fun store(values: CurrencyIdWithQuote) {
        if (values.isEmpty()) return

        coroutineScope {
            launch { storeInRuntime(values = values) }
            launch { storeInPersistence(values = values) }
        }
    }

    private suspend fun storeInRuntime(values: CurrencyIdWithQuote) {
        val quotes = QuoteStatusConverter(isCached = false).convertSet(input = values.entries)

        runtimeStore.update(default = emptySet()) { saved ->
            saved.addOrReplace(items = quotes) { prev, new -> prev.rawCurrencyId == new.rawCurrencyId }
        }
    }

    private suspend fun storeInPersistence(values: CurrencyIdWithQuote) {
        persistenceDataStore.updateData { storedQuotes -> storedQuotes + values }
    }
}