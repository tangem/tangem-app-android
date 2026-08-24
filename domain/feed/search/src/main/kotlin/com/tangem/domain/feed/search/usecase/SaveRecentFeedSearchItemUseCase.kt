package com.tangem.domain.feed.search.usecase

import com.tangem.domain.feed.search.model.RecentFeedSearchItem
import com.tangem.domain.feed.search.repository.FeedSearchHistoryRepository

/** Called by a search tab when the user opens one of its results. */
class SaveRecentFeedSearchItemUseCase(
    private val repository: FeedSearchHistoryRepository,
) {

    suspend operator fun invoke(item: RecentFeedSearchItem) {
        repository.saveRecentItem(item)
    }
}