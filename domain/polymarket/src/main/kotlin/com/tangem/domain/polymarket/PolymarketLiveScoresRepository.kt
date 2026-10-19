package com.tangem.domain.polymarket

import com.tangem.domain.polymarket.model.PolymarketLiveScore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PolymarketLiveScoresRepository {

    /** Every matching frame is delivered, including repeats that refresh lastSeenAt. */
    fun observeLiveScores(gameIds: Set<String>): Flow<PolymarketLiveScore>

    /** Monotonic handshake revision; observing this flow alone does not hold a socket connection. */
    fun observeConnectionRevision(): StateFlow<Long>
}