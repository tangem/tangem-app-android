package com.tangem.data.quotes.multi

import arrow.core.Option
import arrow.core.none
import com.tangem.data.quotes.store.QuotesStatusesStore
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.core.flow.FlowProducerTools.Companion.shareInProducer
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.quotes.multi.MultiQuoteStatusProducer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal class DefaultMultiQuoteStatusProducer @AssistedInject constructor(
    @Assisted val params: Unit,
    private val quotesStatusesStore: QuotesStatusesStore,
    private val dispatchers: CoroutineDispatcherProvider,
    private val producerTools: FlowProducerTools,
) : MultiQuoteStatusProducer {

    override val fallback: Option<Map<CryptoCurrency.RawID, QuoteStatus>> = none()

    override fun produce(): Flow<Map<CryptoCurrency.RawID, QuoteStatus>> {
        return quotesStatusesStore.get()
            .map { statuses -> statuses.associateBy(QuoteStatus::rawCurrencyId) }
            .distinctUntilChanged()
            .flowOn(dispatchers.default)
            .shareInProducer(producerTools, this)
    }

    @AssistedFactory
    interface Factory : MultiQuoteStatusProducer.Factory {
        override fun create(params: Unit): DefaultMultiQuoteStatusProducer
    }
}