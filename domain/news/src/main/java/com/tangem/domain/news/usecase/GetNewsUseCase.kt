package com.tangem.domain.news.usecase

import arrow.core.Either
import com.tangem.domain.models.news.ShortArticle
import com.tangem.domain.news.model.NewsListConfig
import com.tangem.domain.news.repository.NewsRepository

class GetNewsUseCase(private val repository: NewsRepository) {

    suspend fun getNews(limit: Int, newsListConfig: NewsListConfig): Either<Throwable, List<ShortArticle>> =
        Either.catch {
            repository.getNews(
                config = newsListConfig,
                limit = limit,
            )
        }
}