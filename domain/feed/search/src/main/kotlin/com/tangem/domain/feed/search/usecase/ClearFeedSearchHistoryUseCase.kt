package com.tangem.domain.feed.search.usecase

import com.tangem.domain.feed.search.repository.FeedSearchHistoryRepository

/** Clears both recently opened results and recent queries. */
class ClearFeedSearchHistoryUseCase(
    private val repository: FeedSearchHistoryRepository,
) {

    suspend operator fun invoke() {
        repository.clear()
    }
}