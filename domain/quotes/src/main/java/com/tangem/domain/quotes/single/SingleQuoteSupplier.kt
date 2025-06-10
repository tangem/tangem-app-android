package com.tangem.domain.quotes.single

import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.models.quote.QuoteStatus

/**
 * Supplier of quote [SingleQuoteProducer.Params]
 *
 * @property factory    factory for creating [SingleQuoteProducer]
 * @property keyCreator key creator
 *
[REDACTED_AUTHOR]
 */
abstract class SingleQuoteSupplier(
    override val factory: SingleQuoteProducer.Factory,
    override val keyCreator: (SingleQuoteProducer.Params) -> String,
) : FlowCachingSupplier<SingleQuoteProducer, SingleQuoteProducer.Params, QuoteStatus>()