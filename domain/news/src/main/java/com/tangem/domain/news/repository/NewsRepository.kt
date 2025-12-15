package com.tangem.domain.news.repository

import com.tangem.domain.news.model.NewsListBatchFlow
import com.tangem.domain.news.model.NewsListBatchingContext
import com.tangem.domain.models.news.ArticleCategory
import com.tangem.domain.models.news.DetailedArticle
import com.tangem.domain.models.news.ShortArticle
import kotlinx.coroutines.flow.Flow

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
     * Observes cached detailed articles.
     */
    fun observeDetailedArticles(): Flow<Map<Int, DetailedArticle>>

    /**
     * Fetches and caches detailed articles for provided ids in parallel.
     */
    suspend fun fetchDetailedArticles(newsIds: Collection<Int>, language: String?)

    /**
     * Fetch list of trending news by limit and with correct locale and store it in runtime data store.
     *
     * @param limit
     * @param language current device locale
     */
    suspend fun getTrendingNews(limit: Int, language: String?)

    /**
     * Observes trending news with runtime viewed flag support.
     */
    fun observeTrendingNews(): Flow<List<ShortArticle>>

    /**
     * Refreshes trending news list and updates cache without overriding viewed status.
     */
    suspend fun refreshTrendingNews(limit: Int, language: String?)

    /**
     * Updates viewed flag for provided trending articles.
     */
    suspend fun updateTrendingNewsViewed(articleIds: Collection<Int>, viewed: Boolean)

    /**
     * Returns available categories.
     */
    suspend fun getCategories(): List<ArticleCategory>
}