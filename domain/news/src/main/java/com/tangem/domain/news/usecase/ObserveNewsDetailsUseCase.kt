package com.tangem.domain.news.usecase

import com.tangem.domain.models.news.DetailedArticle
import com.tangem.domain.news.repository.NewsRepository
import kotlinx.coroutines.flow.Flow

/**
 * Provides a hot stream with cached detailed articles and supports proactive prefetching.
 *
 * Subscribe once per screen to receive updates for any article that was fetched through [prefetch].
 */
class ObserveNewsDetailsUseCase(
    private val repository: NewsRepository,
) {

    /**
     * Observes all cached detailed articles mapped by id.
     */
    operator fun invoke(): Flow<Map<Int, DetailedArticle>> {
        return repository.observeDetailedArticles()
    }

    /**
     * Prefetches the given article ids (can be called with current + next ids for pager preloading).
     */
    suspend fun prefetch(newsIds: Collection<Int>, language: String?) {
        repository.fetchDetailedArticles(newsIds, language)
    }
}