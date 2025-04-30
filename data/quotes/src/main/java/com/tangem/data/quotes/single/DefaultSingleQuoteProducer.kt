package com.tangem.data.quotes.single

import com.tangem.data.quotes.store.QuotesStoreV2
import com.tangem.domain.quotes.single.SingleQuoteProducer
import com.tangem.domain.tokens.model.Quote
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
    private val quotesStore: QuotesStoreV2,
    private val dispatchers: CoroutineDispatcherProvider,
) : SingleQuoteProducer {

    override val fallback: Quote = Quote.Empty(rawCurrencyId = params.rawCurrencyId)

    override fun produce(): Flow<Quote> {
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