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
    // TODO apply interval parameter AND-8022
    operator fun invoke(
        currencyID: CryptoCurrency.ID,
        interval: PriceChangeInterval,
        refresh: Boolean,
    ): Flow<Option<Quote>> {
        return quotesRepository.getQuotesUpdates(
            currenciesIds = setOf(currencyID),
            refresh = refresh,
        ).map { it.firstOrNull().toOption() }.catch { emit(None) }
    }
}
