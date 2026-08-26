package com.tangem.domain.feed.search.usecase

import com.tangem.domain.feed.search.repository.FeedSearchHistoryRepository

class SaveFeedSearchQueryUseCase(
    private val repository: FeedSearchHistoryRepository,
) {

    suspend operator fun invoke(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        repository.saveRecentQuery(trimmed)
    }
}