package com.tangem.data.news.repository

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.news.NewsApi
import com.tangem.datasource.api.news.models.response.NewsTrendingResponse
import com.tangem.datasource.local.news.details.NewsDetailsStore
import com.tangem.datasource.local.news.trending.TrendingNewsStore
import com.tangem.domain.models.news.*
import com.tangem.domain.news.model.NewsListBatchFlow
import com.tangem.domain.news.model.NewsListBatchingContext
import com.tangem.domain.news.model.NewsListConfig
import com.tangem.domain.news.repository.NewsRepository
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.exception.EndOfPaginationException
import com.tangem.pagination.fetcher.BatchFetcher
import com.tangem.pagination.toBatchFlow
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Implementation of [NewsRepository].
[REDACTED_AUTHOR]
 */
internal class DefaultNewsRepository(
    private val newsApi: NewsApi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val newsDetailsStore: NewsDetailsStore,
    private val trendingNewsStore: TrendingNewsStore,
) : NewsRepository {

    override fun getNewsListBatchFlow(context: NewsListBatchingContext, batchSize: Int): NewsListBatchFlow {
        return BatchListSource(
            fetchDispatcher = dispatchers.io,
            context = context,
            generateNewKey = { keys -> keys.lastOrNull()?.inc() ?: INITIAL_BATCH_KEY },
            batchFetcher = createBatchFetcher(batchSize),
        ).toBatchFlow()
    }

    override suspend fun getDetailedArticle(newsId: Int, language: String?): DetailedArticle {
        val cached = newsDetailsStore.getSyncOrNull(newsId)
        if (cached != null) return cached

        fetchDetailedArticlesInternal(newsIds = listOf(newsId), language = language)

        return requireNotNull(newsDetailsStore.getSyncOrNull(newsId)) {
            "Unable to load detailed article with id=$newsId"
        }
    }

    override fun observeDetailedArticles(): Flow<Map<Int, DetailedArticle>> {
        return newsDetailsStore.getAll().map { articles ->
            articles.associateBy(DetailedArticle::id)
        }
    }

    override suspend fun fetchDetailedArticles(newsIds: Collection<Int>, language: String?) {
        fetchDetailedArticlesInternal(newsIds = newsIds, language = language)
    }

    override suspend fun fetchTrendingNews(limit: Int, language: String?) {
        fetchAndStoreTrendingNews(limit = limit, language = language)
    }

    override fun observeTrendingNews(): Flow<TrendingNews> {
        return trendingNewsStore.get(TRENDING_NEWS_KEY)
    }

    override suspend fun updateTrendingNewsViewed(articleIds: Collection<Int>, viewed: Boolean) {
        if (articleIds.isEmpty()) return

        val currentResult = trendingNewsStore.getSyncOrNull(TRENDING_NEWS_KEY) ?: return
        val currentArticles = when (currentResult) {
            is TrendingNews.Data -> currentResult.articles
            is TrendingNews.Error -> return
        }
        if (currentArticles.isEmpty()) return

        val ids = articleIds.toSet()
        val updated = currentArticles.map { article ->
            if (article.id in ids) {
                article.copy(viewed = viewed)
            } else {
                article
            }
        }

        trendingNewsStore.store(TRENDING_NEWS_KEY, TrendingNews.Data(updated))
    }

    override suspend fun getCategories(): List<ArticleCategory> {
        return newsApi.getCategories().getOrThrow().items.map { dto ->
            ArticleCategory(
                id = dto.id,
                name = dto.name,
            )
        }
    }

    private suspend fun fetchDetailedArticlesInternal(newsIds: Collection<Int>, language: String?) =
        withContext(dispatchers.io) {
            if (newsIds.isEmpty()) return@withContext

            val uniqueIds = newsIds.distinct()
            val idsToFetch = buildList {
                for (id in uniqueIds) {
                    val cached = newsDetailsStore.getSyncOrNull(id)
                    if (cached == null) add(id)
                }
            }

            if (idsToFetch.isEmpty()) return@withContext

            val fetchedArticles = coroutineScope {
                idsToFetch.map { newsId ->
                    async {
                        newsApi.getNewsDetails(newsId = newsId, language = language)
                            .getOrThrow()
                            .toDomainDetailedArticle()
                    }
                }.awaitAll()
            }

            if (fetchedArticles.isNotEmpty()) {
                newsDetailsStore.store(
                    articles = fetchedArticles.associateBy(DetailedArticle::id),
                )
            }
        }

    private suspend fun fetchAndStoreTrendingNews(limit: Int, language: String?) {
        return withContext(dispatchers.io) {
            val apiResponse = newsApi.getTrendingNews(limit = limit, language = language)
            when (val result = apiResponse) {
                is ApiResponse.Error -> {
                    Timber.e(
                        result.cause.cause,
                        "Trending news fetch failed cause: ${
                            when (val error = result.cause) {
                                is ApiResponseError.HttpException -> error.code
                                is ApiResponseError.NetworkException -> "NetworkException"
                                is ApiResponseError.TimeoutException -> "TimeoutException"
                                is ApiResponseError.UnknownException -> "UnknownException"
                            }
                        }",
                    )
                    trendingNewsStore.clear()
                    trendingNewsStore.store(
                        key = TRENDING_NEWS_KEY,
                        value = TrendingNews.Error(
                            NewsError.Unknown(
                                message = result.cause.message,
                                code = null,
                            ),
                        ),
                    )
                }
                is ApiResponse.Success<NewsTrendingResponse> -> {
                    val freshArticles = result.data.items.map { it.toDomainShortArticle() }
                    val cachedArticles = trendingNewsStore.getSyncOrNull(TRENDING_NEWS_KEY)
                    val currentArticles = when (cachedArticles) {
                        is TrendingNews.Data -> cachedArticles.articles
                        is TrendingNews.Error -> emptyList()
                        null -> emptyList()
                    }
                    val merged = mergeTrendingArticles(current = currentArticles, fresh = freshArticles).take(limit)
                    trendingNewsStore.store(TRENDING_NEWS_KEY, TrendingNews.Data(merged))
                    TrendingNews.Data(merged)
                }
            }
        }
    }

    private fun mergeTrendingArticles(current: List<ShortArticle>, fresh: List<ShortArticle>): List<ShortArticle> {
        if (current.isEmpty()) return fresh

        val currentById = current.associateBy(ShortArticle::id)

        return fresh.map { article ->
            val stored = currentById[article.id] ?: return@map article
            article.copy(viewed = stored.viewed)
        }
    }

    private fun createBatchFetcher(batchSize: Int): BatchFetcher<NewsListConfig, List<ShortArticle>> {
        return NewsBatchFetcher(
            newsApi = newsApi,
            batchSize = batchSize,
        )
    }

    private class NewsBatchFetcher(
        private val newsApi: NewsApi,
        private val batchSize: Int,
    ) : BatchFetcher<NewsListConfig, List<ShortArticle>> {

        private var state: NewsPaginationState? = null

        override suspend fun fetchFirst(requestParams: NewsListConfig): BatchFetchResult<List<ShortArticle>> {
            return runSuspendCatching {
                loadPage(
                    page = FIRST_PAGE,
                    params = requestParams,
                    limit = batchSize,
                    snapshotOverride = requestParams.snapshot,
                )
            }.fold(
                onSuccess = { result ->
                    state = result.state
                    result.batchResult
                },
                onFailure = { throwable -> BatchFetchResult.Error(throwable) },
            )
        }

        override suspend fun fetchNext(
            overrideRequestParams: NewsListConfig?,
            lastResult: BatchFetchResult<List<ShortArticle>>,
        ): BatchFetchResult<List<ShortArticle>> {
            val currentState = state ?: return BatchFetchResult.Error(
                IllegalStateException("fetchFirst must be called"),
            )

            if (lastResult is BatchFetchResult.Success && lastResult.last && overrideRequestParams == null) {
                return BatchFetchResult.Error(EndOfPaginationException())
            }

            val params = overrideRequestParams ?: currentState.params
            val shouldReset = overrideRequestParams != null && overrideRequestParams != currentState.params
            val pageToLoad = if (shouldReset) FIRST_PAGE else currentState.nextPage
            val snapshot = if (shouldReset) overrideRequestParams.snapshot else currentState.snapshot

            return runSuspendCatching {
                loadPage(
                    page = pageToLoad,
                    params = params,
                    limit = batchSize,
                    snapshotOverride = snapshot,
                )
            }.fold(
                onSuccess = { result ->
                    state = result.state
                    result.batchResult
                },
                onFailure = { throwable -> BatchFetchResult.Error(throwable) },
            )
        }

        private suspend fun loadPage(
            page: Int,
            params: NewsListConfig,
            limit: Int,
            snapshotOverride: String?,
        ): PageLoadResult {
            val response = newsApi.getNews(
                page = page,
                limit = limit,
                language = params.language,
                snapshot = snapshotOverride,
                tokenIds = params.tokenIds.takeIf { it.isNotEmpty() },
                categoryIds = params.categoryIds.takeIf { it.isNotEmpty() },
            ).getOrThrow()

            val items = response.items.map { it.toDomainShortArticle() }

            val batchResult = BatchFetchResult.Success(
                data = items,
                empty = items.isEmpty(),
                last = response.meta.hasNext.not(),
            )

            return PageLoadResult(
                batchResult = batchResult,
                state = NewsPaginationState(
                    nextPage = response.meta.page + 1,
                    snapshot = response.meta.asOf,
                    params = params.copy(snapshot = response.meta.asOf),
                ),
            )
        }
    }

    private data class PageLoadResult(
        val batchResult: BatchFetchResult.Success<List<ShortArticle>>,
        val state: NewsPaginationState,
    )

    private data class NewsPaginationState(
        val nextPage: Int,
        val snapshot: String?,
        val params: NewsListConfig,
    )

    private companion object {
        private const val INITIAL_BATCH_KEY = 0
        private const val FIRST_PAGE = 1
        private const val TRENDING_NEWS_KEY = "trending_news"
    }
}