package com.tangem.domain.news.repository

import com.tangem.domain.models.news.ArticleCategory
import com.tangem.domain.models.news.DetailedArticle
import com.tangem.domain.models.news.ShortArticle
import com.tangem.domain.models.news.TrendingNews
import com.tangem.domain.news.model.NewsListBatchFlow
import com.tangem.domain.news.model.NewsListBatchingContext
import com.tangem.domain.news.model.NewsListConfig
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
     * Returns flow of list of short article by config.
     *
     * @param config config for getting news list
     */
    fun getNews(config: NewsListConfig, limit: Int): Flow<List<ShortArticle>>

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
    suspend fun fetchTrendingNews(limit: Int, language: String?)

    /**
     * Observes trending news with runtime viewed flag support.
     */
    fun observeTrendingNews(): Flow<TrendingNews>

    /**
     * Returns available categories.
     */
    suspend fun getCategories(): List<ArticleCategory>

    /**
     * Updates viewed flag for provided news articles (applies to both regular and trending news).
     */
    suspend fun updateNewsViewed(articleIds: Collection<Int>, viewed: Boolean)
}