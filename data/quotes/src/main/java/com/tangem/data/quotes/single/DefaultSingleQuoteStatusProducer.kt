package com.tangem.data.quotes.single

import arrow.core.Option
import arrow.core.some
import com.tangem.data.quotes.store.QuotesStatusesStore
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull

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

    private val default = QuoteStatus(rawCurrencyId = params.rawCurrencyId)
    override val fallback: Option<QuoteStatus> = default.some()

    override fun produce(): Flow<QuoteStatus> {
        return quotesStatusesStore.get()
            .mapNotNull { quotes -> quotes.firstOrNull { it.rawCurrencyId == params.rawCurrencyId } ?: default }
            .distinctUntilChanged()
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : SingleQuoteStatusProducer.Factory {
        override fun create(params: SingleQuoteStatusProducer.Params): DefaultSingleQuoteStatusProducer
    }
}