package com.tangem.domain.markets

import arrow.core.None
import arrow.core.Option
import arrow.core.toOption
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.quotes.single.SingleQuoteProducer
import com.tangem.domain.quotes.single.SingleQuoteSupplier
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.repository.QuotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class GetCurrencyQuotesUseCase(
    private val quotesRepository: QuotesRepository,
    private val singleQuoteSupplier: SingleQuoteSupplier,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {
    // TODO apply interval parameter [REDACTED_TASK_KEY]
    operator fun invoke(
        currencyID: CryptoCurrency.ID,
        interval: PriceChangeInterval,
        refresh: Boolean,
    ): Flow<Option<Quote.Value>> {
        val rawId = currencyID.rawCurrencyId ?: return flowOf(None)

        return if (tokensFeatureToggles.isQuotesLoadingRefactoringEnabled) {
            singleQuoteSupplier(
                params = SingleQuoteProducer.Params(rawCurrencyId = rawId),
            )
                .map { (it as? Quote.Value).toOption() }
        } else {
            quotesRepository.getQuotesUpdatesLegacy(
                currenciesIds = setOf(rawId),
                refresh = refresh,
            )
                .map { it.filterIsInstance<Quote.Value>().firstOrNull().toOption() }
        }
            .catch { emit(None) }
    }
}