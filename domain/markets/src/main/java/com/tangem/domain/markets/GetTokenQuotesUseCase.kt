package com.tangem.domain.markets

import arrow.core.Either
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.repository.QuotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Suppress("UnusedPrivateMember")
class GetTokenQuotesUseCase(
    private val quotesRepository: QuotesRepository,
) {
    operator fun invoke(tokenId: String, interval: PriceChangeInterval): Flow<Either<Unit, Quote>> {
        return flowOf(
            Either.catch {
                Quote(
                    rawCurrencyId = "USD",
                    fiatRate = 100.toBigDecimal(), // mock
                    priceChange = 10.toBigDecimal(), // mock
                )
            }.mapLeft {},
        )
        // TODO implement quotes fetching from repository [REDACTED_TASK_KEY]
        // quotesRepository.getQuotesUpdates()
    }
}