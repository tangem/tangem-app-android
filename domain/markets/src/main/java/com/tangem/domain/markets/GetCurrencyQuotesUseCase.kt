package com.tangem.domain.markets

import arrow.core.*
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.repository.QuotesRepository
import kotlinx.coroutines.flow.*

@Suppress("UnusedPrivateMember")
class GetCurrencyQuotesUseCase(
    private val quotesRepository: QuotesRepository,
) {
    // TODO apply interval parameter [REDACTED_TASK_KEY]
    operator fun invoke(
        currencyID: CryptoCurrency.ID,
        interval: PriceChangeInterval,
        refresh: Boolean,
    ): Flow<Option<Quote.Value>> {
        val rawId = currencyID.rawCurrencyId ?: return flowOf(None)

        return quotesRepository.getQuotesUpdates(
            currenciesIds = setOf(rawId),
            refresh = refresh,
        ).map { it.filterIsInstance<Quote.Value>().firstOrNull().toOption() }.catch { emit(None) }
    }
}