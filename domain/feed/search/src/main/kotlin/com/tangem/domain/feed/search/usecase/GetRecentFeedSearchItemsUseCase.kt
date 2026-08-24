package com.tangem.domain.feed.search.usecase

import com.tangem.domain.feed.search.model.RecentFeedSearchItem
import com.tangem.domain.feed.search.repository.FeedSearchHistoryRepository
import kotlinx.coroutines.flow.Flow

class GetRecentFeedSearchItemsUseCase(
    private val repository: FeedSearchHistoryRepository,
) {

    operator fun invoke(): Flow<List<RecentFeedSearchItem>> = repository.getRecentItems()
}