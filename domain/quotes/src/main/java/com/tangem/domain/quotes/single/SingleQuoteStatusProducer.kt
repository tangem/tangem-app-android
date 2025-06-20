package com.tangem.domain.quotes.single

import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus

/**
 * Producer of quote [CryptoCurrency.RawID]
 *
* [REDACTED_AUTHOR]
 */
interface SingleQuoteStatusProducer : FlowProducer<QuoteStatus> {

    data class Params(val rawCurrencyId: CryptoCurrency.RawID)

    interface Factory : FlowProducer.Factory<Params, SingleQuoteStatusProducer>
}
