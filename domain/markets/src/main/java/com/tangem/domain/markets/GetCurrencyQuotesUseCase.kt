package com.tangem.domain.markets

import arrow.core.None
import arrow.core.Option
import arrow.core.toOption
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.repository.QuotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

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

        return quotesRepository.getQuotesUpdatesLegacy(
            currenciesIds = setOf(rawId),
            refresh = refresh,
        ).map { it.filterIsInstance<Quote.Value>().firstOrNull().toOption() }.catch { emit(None) }
    }
}