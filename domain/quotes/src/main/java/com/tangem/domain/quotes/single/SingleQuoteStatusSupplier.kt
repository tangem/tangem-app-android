package com.tangem.domain.quotes.single

import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.models.quote.QuoteStatus

/**
 * Supplier of quote [SingleQuoteStatusProducer.Params]
 *
 * @property factory    factory for creating [SingleQuoteStatusProducer]
 * @property keyCreator key creator
 *
[REDACTED_AUTHOR]
 */
abstract class SingleQuoteStatusSupplier(
    override val factory: SingleQuoteStatusProducer.Factory,
    override val keyCreator: (SingleQuoteStatusProducer.Params) -> String,
) : FlowCachingSupplier<SingleQuoteStatusProducer, SingleQuoteStatusProducer.Params, QuoteStatus>()