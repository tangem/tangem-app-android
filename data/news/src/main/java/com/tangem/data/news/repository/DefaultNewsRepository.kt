package com.tangem.data.news.repository

import arrow.core.Either
import arrow.core.flatten
import arrow.core.left
import arrow.core.right
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.fold
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.news.NewsApi
import com.tangem.datasource.api.news.models.response.NewsTrendingResponse
import com.tangem.datasource.local.news.details.NewsDetailsStore
import com.tangem.datasource.local.news.liked.NewsLikedStore
import com.tangem.datasource.local.news.trending.TrendingNewsStore
import com.tangem.datasource.local.news.viewed.NewsViewedStore
import com.tangem.domain.models.news.ArticleCategory
import com.tangem.domain.models.news.DetailedArticle
import com.tangem.domain.models.news.ShortArticle
import com.tangem.domain.models.news.TrendingNews
import com.tangem.domain.news.NewsErrorResolver
import com.tangem.domain.news.model.NewsListBatchFlow
import com.tangem.domain.news.model.NewsListBatchingContext
import com.tangem.domain.news.model.NewsListConfig
import com.tangem.domain.news.repository.NewsRepository
import com.tangem.pagination.*
import com.tangem.pagination.exception.EndOfPaginationException
import com.tangem.pagination.fetcher.BatchFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

/**
 * Implementation of [NewsRepository].
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
internal class DefaultNewsRepository(
    private val newsApi: NewsApi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val newsDetailsStore: NewsDetailsStore,
    private val trendingNewsStore: TrendingNewsStore,
    private val newsViewedStore: NewsViewedStore,
    private val newsLikedStore: NewsLikedStore,
    private val newsErrorResolver: NewsErrorResolver,
) : NewsRepository {

    override fun getNewsListBatchFlow(context: NewsListBatchingContext, batchSize: Int): NewsListBatchFlow {
        val newsBatchFlow = BatchListSource(
            fetchDispatcher = dispatchers.io,
            context = context,
            generateNewKey = { keys -> keys.lastOrNull()?.inc() ?: INITIAL_BATCH_KEY },
            batchFetcher = createBatchFetcher(batchSize),
        ).toBatchFlow()

        return updateViewedStatusForNewsBatch(newsBatchFlow, context.coroutineScope)
    }

    override fun getNews(config: NewsListConfig, limit: Int): Flow<Either<Throwable, List<ShortArticle>>> {
        return flow {
            val items = newsApi.getNews(
                page = FIRST_PAGE,
                limit = limit,
                language = config.language,
                snapshot = config.snapshot,
                tokenIds = config.tokenIds.takeIf { it.isNotEmpty() },
                categoryIds = config.categoryIds.takeIf { it.isNotEmpty() },
            ).fold(
                onSuccess = { response ->
                    response.items
                },
                onError = { error ->
                    Timber.e(error, "Failed to get list of news")
                    throw error
                },
            )

            val shortArticles = items.map { it.toDomainShortArticle() }
            emit(shortArticles)
        }
            .flowOn(dispatchers.io)
            .combine(newsViewedStore.getAll()) { articlesToUpdate, viewedFlags ->
                articlesToUpdate.map { article ->
                    val isViewed = viewedFlags[article.id] == true
                    article.copy(viewed = isViewed)
                }
            }
            .map<List<ShortArticle>, Either<Throwable, List<ShortArticle>>> { it.right() }
            .catch { emit(it.left()) }
    }

    override fun observeDetailedArticles(): Flow<Map<Int, DetailedArticle>> {
        return newsDetailsStore.getAll()
            .combine(newsLikedStore.getAll()) { articles, likedFlags ->
                articles.map { article ->
                    article.copy(
                        isLiked = likedFlags[article.id] == true,
                    )
                }
            }
            .map { articles ->
                articles.associateBy(DetailedArticle::id)
            }
    }

    override suspend fun fetchDetailedArticles(
        newsIds: Collection<Int>,
        language: String?,
    ): Either<Map<Int, Throwable>, Unit> = fetchDetailedArticlesInternal(newsIds = newsIds, language = language)

    override suspend fun fetchTrendingNews(limit: Int, language: String?) {
        fetchAndStoreTrendingNews(limit = limit, language = language)
    }

    override fun observeTrendingNews(): Flow<TrendingNews> {
        return combine(
            trendingNewsStore.get(TRENDING_NEWS_KEY),
            newsViewedStore.getAll(),
        ) { trendingNews, viewedFlags ->
            when (trendingNews) {
                is TrendingNews.Data -> {
                    val articlesWithViewedFlags = trendingNews.articles
                        .map { article ->
                            article.copy(viewed = viewedFlags[article.id] == true)
                        }
                        .sortedBy { it.viewed }
                    TrendingNews.Data(articlesWithViewedFlags)
                }
                is TrendingNews.Error -> trendingNews
            }
        }
    }

    override suspend fun getCategories(): List<ArticleCategory> {
        return withContext(dispatchers.io) {
            newsApi.getCategories().getOrThrow().items.map { dto ->
                ArticleCategory(
                    id = dto.id,
                    name = dto.name,
                )
            }
        }
    }

    override suspend fun updateNewsViewed(articleIds: Collection<Int>, viewed: Boolean) {
        newsViewedStore.updateViewed(articleIds, viewed)
    }

    private suspend fun isNewsLiked(articleId: Int): Boolean {
        return newsLikedStore.getSync()[articleId] == true
    }

    override suspend fun toggleNewsLiked(articleId: Int) = withContext(dispatchers.io) {
        val isNewsLikedValue = !isNewsLiked(articleId)
        newsLikedStore.updateLiked(listOf(articleId), isNewsLikedValue)
    }

    private fun updateViewedStatusForNewsBatch(
        newsBatchFlow: NewsListBatchFlow,
        scope: CoroutineScope,
    ): NewsListBatchFlow {
        return NewsBatchFlowWithViewedStatus(
            upstream = newsBatchFlow,
            newsViewedStore = newsViewedStore,
            scope = scope,
        )
    }

    private suspend fun fetchDetailedArticlesInternal(
        newsIds: Collection<Int>,
        language: String?,
    ): Either<Map<Int, Throwable>, Unit> = Either.catch {
        withContext(dispatchers.io) {
            if (newsIds.isEmpty()) return@withContext Unit.right()

            val uniqueIds = newsIds.distinct()
            val idsToFetch = buildList {
                for (id in uniqueIds) {
                    val cached = newsDetailsStore.getSyncOrNull(id)
                    if (cached == null) add(id)
                }
            }

            if (idsToFetch.isEmpty()) return@withContext Unit.right()

            val fetchedArticles = supervisorScope {
                idsToFetch.map { newsId ->
                    async {
                        Either.catch {
                            newsApi.getNewsDetails(newsId = newsId, language = language)
                                .getOrThrow()
                                .toDomainDetailedArticle(
                                    isLiked = isNewsLiked(newsId),
                                )
                        }.mapLeft {
                            newsId to it
                        }
                    }
                }.awaitAll()
            }

            fetchedArticles
                .filterIsInstance<Either.Right<DetailedArticle>>()
                .map { it.value }
                .let { articles ->
                    if (articles.isNotEmpty()) {
                        newsDetailsStore.store(
                            articles = articles.associateBy(DetailedArticle::id),
                        )
                    }
                }

            val errors = fetchedArticles
                .filterIsInstance<Either.Left<Pair<Int, Throwable>>>()
                .associate { it.value.first to it.value.second }

            if (errors.isEmpty()) Unit.right() else errors.left()
        }
    }.mapLeft { t -> mapOf(GLOBAL_ERROR_ID to t) }.flatten()

    private suspend fun fetchAndStoreTrendingNews(limit: Int, language: String?) {
        return withContext(dispatchers.io) {
            when (val apiResponse = newsApi.getTrendingNews(limit = limit, language = language)) {
                is ApiResponse.Error -> {
                    Timber.e(
                        apiResponse.cause,
                        "Trending news fetch failed cause: ${
                            when (val error = apiResponse.cause) {
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
                        value = TrendingNews.Error(error = newsErrorResolver.resolve(apiResponse.cause)),
                    )
                }
                is ApiResponse.Success<NewsTrendingResponse> -> {
                    val freshArticles = apiResponse.data.items.map { it.toDomainShortArticle() }
                    val articles = freshArticles.take(limit)
                    trendingNewsStore.store(TRENDING_NEWS_KEY, TrendingNews.Data(articles))
                    TrendingNews.Data(articles)
                }
            }
        }
    }

    private fun createBatchFetcher(batchSize: Int): BatchFetcher<NewsListConfig, List<ShortArticle>> {
        return NewsBatchFetcher(
            newsApi = newsApi,
            batchSize = batchSize,
            newsViewedStore = newsViewedStore,
        )
    }

    private class NewsBatchFetcher(
        private val newsApi: NewsApi,
        private val batchSize: Int,
        private val newsViewedStore: NewsViewedStore,
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
                snapshot = snapshotOverride?.takeIf { it.isNotEmpty() },
                tokenIds = params.tokenIds.takeIf { it.isNotEmpty() },
                categoryIds = params.categoryIds.takeIf { it.isNotEmpty() },
            ).getOrThrow()

            val articles = response.items.map { it.toDomainShortArticle() }
            val viewedFlags = newsViewedStore.getSync()

            val items = articles.map { article ->
                val isViewed = viewedFlags[article.id] == true
                article.copy(viewed = isViewed)
            }

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

    private class NewsBatchFlowWithViewedStatus(
        private val upstream: NewsListBatchFlow,
        newsViewedStore: NewsViewedStore,
        scope: CoroutineScope,
    ) : NewsListBatchFlow {

        override val state: StateFlow<BatchListState<Int, List<ShortArticle>>> = combine(
            upstream.state,
            newsViewedStore.getAll(),
        ) { batchListState, viewedFlags ->
            val updatedBatches = batchListState.data.map { batch ->
                val updatedArticles = batch.data.map { article ->
                    val isViewed = viewedFlags[article.id] == true
                    article.copy(viewed = isViewed)
                }
                Batch(key = batch.key, data = updatedArticles)
            }
            BatchListState(
                data = updatedBatches,
                status = batchListState.status,
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = BatchListState(emptyList(), upstream.state.value.status),
        )

        override val updateResults: SharedFlow<Pair<Nothing, BatchUpdateResult<Int, List<ShortArticle>>>>
            get() = upstream.updateResults
    }

    private companion object {
        private const val INITIAL_BATCH_KEY = 0
        private const val FIRST_PAGE = 1
        private const val TRENDING_NEWS_KEY = "trending_news"
        private const val GLOBAL_ERROR_ID = -1
    }
}