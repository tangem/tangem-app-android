package com.tangem.domain.news.usecase

import com.tangem.domain.models.news.ArticleCategory
import com.tangem.domain.news.repository.NewsRepository

/**
 * Returns the list of available news categories.
 *
 * Use this from presentation layer to refresh filters or render category chips.
 */
class GetNewsCategoriesUseCase(
    private val repository: NewsRepository,
) {

    /**
     * Fetches categories from the repository.
     */
    suspend operator fun invoke(): List<ArticleCategory> {
        return repository.getCategories()
    }
}