package com.tangem.domain.markets

import arrow.core.None
import arrow.core.Option
import arrow.core.toOption
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.quotes.single.SingleQuoteProducer
import com.tangem.domain.quotes.single.SingleQuoteSupplier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class GetCurrencyQuotesUseCase(
    private val singleQuoteSupplier: SingleQuoteSupplier,
) {
    // TODO apply interval parameter [REDACTED_TASK_KEY]
    operator fun invoke(
        currencyID: CryptoCurrency.ID,
        interval: PriceChangeInterval,
        refresh: Boolean,
    ): Flow<Option<QuoteStatus.Data>> {
        val rawId = currencyID.rawCurrencyId ?: return flowOf(None)

        return singleQuoteSupplier(
            params = SingleQuoteProducer.Params(rawCurrencyId = rawId),
        )
            .map { (it.value as? QuoteStatus.Data).toOption() }
            .catch { emit(None) }
    }
}