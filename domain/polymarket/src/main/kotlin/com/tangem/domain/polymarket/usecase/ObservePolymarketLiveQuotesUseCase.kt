package com.tangem.domain.polymarket.usecase

import com.tangem.domain.polymarket.PolymarketLiveQuotesRepository
import com.tangem.domain.polymarket.model.PolymarketLiveQuote
import kotlinx.coroutines.flow.Flow

class ObservePolymarketLiveQuotesUseCase(private val repository: PolymarketLiveQuotesRepository) {

    operator fun invoke(assetIds: Set<String>): Flow<Map<String, PolymarketLiveQuote>> =
        repository.observeLiveQuotes(assetIds)
}