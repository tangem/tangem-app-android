package com.tangem.domain.quotes.multi

import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus

@Suppress("UnnecessaryAbstractClass")
abstract class MultiQuoteStatusSupplier(
    override val factory: MultiQuoteStatusProducer.Factory,
    override val keyCreator: (Unit) -> String,
) : FlowCachingSupplier<MultiQuoteStatusProducer, Unit, Map<CryptoCurrency.RawID, QuoteStatus>>()