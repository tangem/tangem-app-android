package com.tangem.domain.polymarket

import com.tangem.domain.polymarket.model.PolymarketLiveQuote
import kotlinx.coroutines.flow.Flow

interface PolymarketLiveQuotesRepository {

    /** REST remains the source for metadata and for prices whose status is not LIVE. */
    fun observeLiveQuotes(assetIds: Set<String>): Flow<Map<String, PolymarketLiveQuote>>
}