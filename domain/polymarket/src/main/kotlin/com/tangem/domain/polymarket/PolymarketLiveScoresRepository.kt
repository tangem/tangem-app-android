package com.tangem.domain.polymarket

import com.tangem.domain.polymarket.model.PolymarketLiveScore
import kotlinx.coroutines.flow.Flow

interface PolymarketLiveScoresRepository {

    /** Every matching frame is delivered, including repeats that refresh lastSeenAt. */
    fun observeLiveScores(gameIds: Set<String>): Flow<PolymarketLiveScore>

    /** Current connection status; observing alone does not hold a connection or guarantee every transition. */
    fun observeConnectionState(): Flow<Boolean>
}