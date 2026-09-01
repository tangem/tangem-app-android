package com.tangem.domain.feed.search.usecase

import com.tangem.domain.feed.search.repository.FeedSearchHistoryRepository

class RemoveFeedSearchQueryUseCase(
    private val repository: FeedSearchHistoryRepository,
) {

    suspend operator fun invoke(query: String) {
        repository.removeRecentQuery(query)
    }
}