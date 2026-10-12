package com.tangem.domain.markets

import com.tangem.domain.markets.repositories.MarketIndicatorsRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for observing the session-cached indicator readings, keyed by uppercase coin symbol.
 * The cache starts empty and is populated by [FetchCoinIndicatorsUseCase], so consumers can render
 * already-stored readings immediately and refresh in the background
 *
 * @property marketIndicatorsRepository repository of coin indicators
 *
[REDACTED_AUTHOR]
 */
class GetCoinIndicatorsUpdatesUseCase(
    private val marketIndicatorsRepository: MarketIndicatorsRepository,
) {

    operator fun invoke(): Flow<Map<String, CoinIndicators>> {
        return marketIndicatorsRepository.getCoinIndicatorsUpdates()
    }
}