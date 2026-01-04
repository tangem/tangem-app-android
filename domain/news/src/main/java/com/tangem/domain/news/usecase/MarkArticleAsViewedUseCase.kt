package com.tangem.domain.news.usecase

import com.tangem.domain.news.repository.NewsRepository

class MarkArticleAsViewedUseCase(private val repository: NewsRepository) {

    /**
     * Marks a single article as viewed/unviewed.
     */
    suspend fun markAsViewed(articleId: Int, viewed: Boolean = true) {
        repository.updateNewsViewed(listOf(articleId), viewed)
    }

    /**
     * Marks multiple articles at once (useful for bulk updates).
     */
    suspend fun markAsViewed(articleIds: Collection<Int>, viewed: Boolean = true) {
        repository.updateNewsViewed(articleIds, viewed)
    }
}