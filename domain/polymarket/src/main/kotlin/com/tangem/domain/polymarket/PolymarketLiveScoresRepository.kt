package com.tangem.domain.polymarket

import com.tangem.domain.polymarket.model.PolymarketLiveScore
import kotlinx.coroutines.flow.Flow

interface PolymarketLiveScoresRepository {

    fun observeLiveScores(gameIds: Set<String>): Flow<PolymarketLiveScore>
}