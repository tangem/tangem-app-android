package com.tangem.domain.news.usecase

import arrow.core.Either
import com.tangem.domain.news.repository.NewsRepository

class ToggleArticleLikedUseCase(private val repository: NewsRepository) {

    /**
     * Toggle an article liked state.
     */
    suspend fun toggleLiked(articleId: Int) = Either.catch {
        repository.toggleNewsLiked(articleId)
    }
}