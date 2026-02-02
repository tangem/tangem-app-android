package com.tangem.domain.news.usecase

import arrow.core.Either
import com.tangem.domain.models.news.ShortArticle
import com.tangem.domain.news.model.NewsListConfig
import com.tangem.domain.news.repository.NewsRepository
import kotlinx.coroutines.flow.Flow

class GetNewsUseCase(private val repository: NewsRepository) {

    fun getNews(limit: Int, newsListConfig: NewsListConfig): Flow<Either<Throwable, List<ShortArticle>>> =
        repository.getNews(
            config = newsListConfig,
            limit = limit,
        )
}