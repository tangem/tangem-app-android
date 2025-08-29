package com.tangem.data.quotes.single

import com.tangem.data.quotes.store.QuotesStatusesStore
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*

/**
 * Default implementation of [SingleQuoteStatusProducer]
 *
 * @property params              params
 * @property quotesStatusesStore quotes store
 */
internal class DefaultSingleQuoteStatusProducer @AssistedInject constructor(
    @Assisted val params: SingleQuoteStatusProducer.Params,
    private val quotesStatusesStore: QuotesStatusesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : SingleQuoteStatusProducer {

    override val fallback: QuoteStatus = QuoteStatus(rawCurrencyId = params.rawCurrencyId)

    override fun produce(): Flow<QuoteStatus> {
        return quotesStatusesStore.get()
            .mapNotNull { quotes -> quotes.firstOrNull { it.rawCurrencyId == params.rawCurrencyId } ?: fallback }
            .distinctUntilChanged()
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : SingleQuoteStatusProducer.Factory {
        override fun create(params: SingleQuoteStatusProducer.Params): DefaultSingleQuoteStatusProducer
    }
}