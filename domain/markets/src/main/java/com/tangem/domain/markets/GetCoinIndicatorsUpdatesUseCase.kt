package com.tangem.domain.markets

import com.tangem.domain.markets.repositories.CoinIndicatorsRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for observing the session-cached indicator readings, keyed by uppercase coin symbol.
 * The cache starts empty and is populated by [FetchCoinIndicatorsUseCase], so consumers can render
 * already-stored readings immediately and refresh in the background
 *
 * @property coinIndicatorsRepository repository of coin indicators
 *
[REDACTED_AUTHOR]
 */
class GetCoinIndicatorsUpdatesUseCase(
    private val coinIndicatorsRepository: CoinIndicatorsRepository,
) {

    operator fun invoke(): Flow<Map<String, CoinIndicators>> {
        return coinIndicatorsRepository.getCoinIndicatorsUpdates()
    }
}