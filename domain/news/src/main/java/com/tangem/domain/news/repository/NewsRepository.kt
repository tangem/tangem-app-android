package com.tangem.domain.news.repository

import com.tangem.domain.news.model.NewsListBatchFlow
import com.tangem.domain.news.model.NewsListBatchingContext
import com.tangem.domain.news.models.ArticleCategory
import com.tangem.domain.news.models.DetailedArticle
import com.tangem.domain.news.models.ShortArticle

/**
[REDACTED_AUTHOR]
 */
interface NewsRepository {

    /**
     * Returns flow of batch with pagination handling.
     *
     * @param context pagination context
     * @param batchSize page capacity
     */
    fun getNewsListBatchFlow(context: NewsListBatchingContext, batchSize: Int): NewsListBatchFlow

    /**
     * Returns detailed article by id with locale configuration.
     * @param newsId news identification
     * @param language current locale
     */
    suspend fun getDetailedArticle(newsId: Int, language: String?): DetailedArticle

    /**
     * Returns list of trending news by limit and with correct locale.
     *
     * @param limit
     * @param language current device locale
     */
    suspend fun getTrendingNews(limit: Int, language: String?): List<ShortArticle>

    /**
     * Returns available categories.
     */
    suspend fun getCategories(): List<ArticleCategory>
}