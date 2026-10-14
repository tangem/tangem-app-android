package com.tangem.domain.polymarket.usecase

import com.tangem.domain.polymarket.PolymarketLiveScoresRepository
import com.tangem.domain.polymarket.model.PolymarketLiveScore
import kotlinx.coroutines.flow.Flow

class ObservePolymarketLiveScoresUseCase(
    private val liveScoresRepository: PolymarketLiveScoresRepository,
) {

    operator fun invoke(gameIds: Set<String>): Flow<PolymarketLiveScore> =
        liveScoresRepository.observeLiveScores(gameIds)
}