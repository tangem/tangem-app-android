package com.tangem.data.quotes.single

import com.tangem.data.quotes.store.QuotesStatusesStore
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.quotes.single.SingleQuoteProducer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull

/**
 * Default implementation of [SingleQuoteProducer]
 *
 * @property params      params
 * @property quotesStore quotes store
 */
internal class DefaultSingleQuoteProducer @AssistedInject constructor(
    @Assisted val params: SingleQuoteProducer.Params,
    private val quotesStore: QuotesStatusesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : SingleQuoteProducer {

    override val fallback: QuoteStatus = QuoteStatus(rawCurrencyId = params.rawCurrencyId)

    override fun produce(): Flow<QuoteStatus> {
        return quotesStore.get()
            .mapNotNull { quotes -> quotes.firstOrNull { it.rawCurrencyId == params.rawCurrencyId } }
            .distinctUntilChanged()
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : SingleQuoteProducer.Factory {
        override fun create(params: SingleQuoteProducer.Params): DefaultSingleQuoteProducer
    }
}