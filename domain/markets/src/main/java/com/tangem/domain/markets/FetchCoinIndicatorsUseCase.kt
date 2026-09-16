package com.tangem.domain.markets

import arrow.core.Either
import com.tangem.domain.markets.repositories.MarketIndicatorsRepository

/**
 * Use case for fetching technical and social indicator readings per coin into the session store.
 * The fetched readings are observed via [GetCoinIndicatorsUpdatesUseCase]
 *
 * @property marketIndicatorsRepository repository of coin indicators
 *
[REDACTED_AUTHOR]
 */
class FetchCoinIndicatorsUseCase(
    private val marketIndicatorsRepository: MarketIndicatorsRepository,
) {

    /**
     * Fetches indicator readings and merges them into the session store
     *
     * @param symbols    coin symbols to filter by (case-insensitive). `null` or empty — all supported coins
     * @param indicators indicator types to filter by. `null` or empty — all indicators
     */
    suspend operator fun invoke(
        symbols: List<String>? = null,
        indicators: List<CoinIndicators.Reading.Type>? = null,
    ): Either<Throwable, Unit> {
        return marketIndicatorsRepository.fetchCoinIndicators(symbols = symbols, indicators = indicators)
    }
}