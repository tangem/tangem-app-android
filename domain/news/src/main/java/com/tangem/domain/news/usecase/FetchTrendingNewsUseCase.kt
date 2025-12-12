package com.tangem.domain.news.usecase

import arrow.core.Either
import com.tangem.domain.news.repository.NewsRepository
import java.util.Locale

/**
 * Fetches trending news to store it in runtime data store.
 */

class FetchTrendingNewsUseCase(private val newsRepository: NewsRepository) {

    suspend operator fun invoke(): Either<Throwable, Unit> = Either.catch {
        newsRepository.getTrendingNews(
            limit = LIMIT_FOR_TRENDING_NEWS,
            language = Locale.getDefault().language,
        )
    }

    companion object {
        private const val LIMIT_FOR_TRENDING_NEWS = 10
    }
}