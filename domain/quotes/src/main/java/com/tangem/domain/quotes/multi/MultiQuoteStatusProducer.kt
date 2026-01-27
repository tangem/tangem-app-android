package com.tangem.domain.quotes.multi

import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus

interface MultiQuoteStatusProducer : FlowProducer<Map<CryptoCurrency.RawID, QuoteStatus>> {

    interface Factory : FlowProducer.Factory<Unit, MultiQuoteStatusProducer>
}