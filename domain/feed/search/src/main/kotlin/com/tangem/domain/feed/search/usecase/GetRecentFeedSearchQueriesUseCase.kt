package com.tangem.domain.feed.search.usecase

import com.tangem.domain.feed.search.repository.FeedSearchHistoryRepository
import kotlinx.coroutines.flow.Flow

class GetRecentFeedSearchQueriesUseCase(
    private val repository: FeedSearchHistoryRepository,
) {

    operator fun invoke(): Flow<List<String>> = repository.getRecentQueries()
}