package com.tangem.domain.news.usecase

import com.tangem.domain.models.news.ShortArticle
import com.tangem.domain.news.repository.NewsRepository
import kotlinx.coroutines.flow.Flow

/**
 * Exposes trending news as a cached flow and provides helpers to refresh or mark items as viewed.
 *
 * Keep a single instance per process (provided via DI) and collect the flow to render trending rows.
 */
class ManageTrendingNewsUseCase(private val repository: NewsRepository) {

    /**
     * Observes the current cached list of trending articles (max 10 items).
     */
    operator fun invoke(): Flow<List<ShortArticle>> {
        return repository.observeTrendingNews()
    }

    /**
     * Forces refresh from backend while preserving local `viewed` flags.
     */
    suspend fun refresh(limit: Int, language: String?) {
        repository.refreshTrendingNews(limit, language)
    }

    /**
     * Marks a single article as viewed/unviewed.
     */
    suspend fun markAsViewed(articleId: Int, viewed: Boolean = true) {
        repository.updateTrendingNewsViewed(listOf(articleId), viewed)
    }

    /**
     * Marks multiple articles at once (useful for bulk updates).
     */
    suspend fun markAsViewed(articleIds: Collection<Int>, viewed: Boolean = true) {
        repository.updateTrendingNewsViewed(articleIds, viewed)
    }
}