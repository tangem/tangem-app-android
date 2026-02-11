package com.tangem.features.feed.model.news.details

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetTokenMarketInfoUseCase
import com.tangem.domain.markets.GetTokenPriceChartUseCase
import com.tangem.domain.models.news.RelatedArticle
import com.tangem.domain.news.usecase.GetNewsListBatchFlowUseCase
import com.tangem.domain.news.usecase.MarkArticleAsViewedUseCase
import com.tangem.domain.news.usecase.ObserveNewsDetailsUseCase
import com.tangem.domain.news.usecase.ToggleArticleLikedUseCase
import com.tangem.features.feed.components.news.details.DefaultNewsDetailsComponent
import com.tangem.features.feed.model.news.details.analytics.NewsDetailsAnalyticsEvent
import com.tangem.features.feed.model.news.details.converter.NewsDetailsConverter
import com.tangem.features.feed.model.news.details.factory.NewsDetailsIndexManager
import com.tangem.features.feed.model.news.details.factory.NewsDetailsStateFactory
import com.tangem.features.feed.model.news.details.loader.NewsRelatedTokensLoader
import com.tangem.features.feed.ui.news.details.state.ArticleUM
import com.tangem.features.feed.ui.news.details.state.ArticlesStateUM
import com.tangem.features.feed.ui.news.details.state.NewsDetailsUM
import com.tangem.features.feed.ui.news.details.state.RelatedTokensUM
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class NewsDetailsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val observeNewsDetailsUseCase: ObserveNewsDetailsUseCase,
    private val getNewsListBatchFlowUseCase: GetNewsListBatchFlowUseCase,
    private val urlOpener: UrlOpener,
    private val shareManager: ShareManager,
    private val getTokenMarketInfoUseCase: GetTokenMarketInfoUseCase,
    private val getTokenPriceChartUseCase: GetTokenPriceChartUseCase,
    private val markArticleAsViewedUseCase: MarkArticleAsViewedUseCase,
    private val toggleArticleLikedUseCase: ToggleArticleLikedUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<DefaultNewsDetailsComponent.Params>()
    private val currentLanguage = Locale.getDefault().language

    private val newsDetailsConverter = NewsDetailsConverter(onRelatedArticleClick = ::onRelatedArticleClick)

    private val paginationManager: NewsDetailsPaginationManager? = params.paginationConfig?.let { config ->
        NewsDetailsPaginationManager(
            getNewsListBatchFlowUseCase = getNewsListBatchFlowUseCase,
            currentLanguage = Provider { config.language },
            currentCategoryIds = Provider { config.categoryIds },
            modelScope = modelScope,
            dispatchers = dispatchers,
            observeNewsDetailsUseCase = observeNewsDetailsUseCase,
            prefetchedIds = params.preselectedArticlesId.toSet(),
        )
    }

    private val relatedTokensLoader by lazy {
        NewsRelatedTokensLoader(
            getTokenMarketInfoUseCase = getTokenMarketInfoUseCase,
            getTokenPriceChartUseCase = getTokenPriceChartUseCase,
            dispatchers = dispatchers,
            maxCount = RELATED_TOKEN_MAX_COUNT,
        )
    }

    private val currentAppCurrency = getSelectedAppCurrencyUseCase().map { maybeAppCurrency ->
        maybeAppCurrency.getOrElse { AppCurrency.Default }
    }
        .flowOn(dispatchers.default)
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppCurrency.Default,
        )

    private val _state = MutableStateFlow(
        NewsDetailsUM(
            articlesStateUM = ArticlesStateUM.Loading,
            articles = persistentListOf(),
            selectedArticleIndex = -1,
            onShareClick = {},
            onLikeClick = ::onLikeClick,
            onBackClick = params.onBackClicked,
            onArticleIndexChanged = ::onArticleIndexChanged,
        ),
    )

    private val stateFactory by lazy(LazyThreadSafetyMode.NONE) {
        NewsDetailsStateFactory(
            currentStateProvider = Provider { _state.value },
            onStateUpdate = { newState -> _state.update { newState } },
            onRetryClick = ::onRetryClicked,
            onShareClick = ::onShareClick,
        )
    }

    val state: StateFlow<NewsDetailsUM> = _state.asStateFlow()

    init {
        trackScreenOpened()
        loadNews()
    }

    private fun trackScreenOpened() {
        analyticsEventHandler.send(
            event = NewsDetailsAnalyticsEvent.NewsArticleOpened(
                source = params.screenSource,
                newsId = params.articleId,
            ),
        )
    }

    private fun onLikeClick(articleId: Int) {
        modelScope.launch {
            toggleArticleLikedUseCase
                .toggleLiked(articleId)
                .onLeft { Timber.e(it) }
            analyticsEventHandler.send(NewsDetailsAnalyticsEvent.NewsLikeClicked(articleId))
        }
    }

    private fun onRelatedArticleClick(relatedArticle: RelatedArticle) {
        analyticsEventHandler.send(
            NewsDetailsAnalyticsEvent.RelatedNewsClicked(
                newsId = params.articleId,
                relatedNewsId = relatedArticle.id,
            ),
        )
        urlOpener.openUrl(relatedArticle.url)
    }

    private fun onShareClick(article: ArticleUM) {
        shareManager.shareText(article.newsUrl)
        analyticsEventHandler.send(NewsDetailsAnalyticsEvent.NewsShareButtonClick(article.id))
    }

    private fun onArticleIndexChanged(newIndex: Int) {
        stateFactory.updateSelectedArticleIndex(newIndex)
        val currentArticle = when (state.value.articlesStateUM) {
            is ArticlesStateUM.Content -> {
                paginationManager?.checkAndLoadMoreIfNeeded(
                    currentIndex = newIndex,
                    totalArticlesCount = state.value.articles.size,
                )
                state.value.articles.getOrNull(newIndex)
            }
            ArticlesStateUM.Loading,
            is ArticlesStateUM.LoadingError,
            -> null
        }
        currentArticle?.let { article ->
            markArticleAsViewed(article.id)
            loadRelatedTokens(article)
        }
    }

    private fun loadNews() {
        modelScope.launch {
            initialPrefetch()
            paginationManager?.start()
            val articleIdsFlow = paginationManager?.cachedPrefetchedIds ?: flowOf(params.preselectedArticlesId)
            combine(
                articleIdsFlow,
                observeNewsDetailsUseCase.invoke(),
            ) { articleIds, articlesMap ->
                articleIds.mapNotNull { articleId ->
                    articlesMap[articleId]?.let { detailedArticle ->
                        newsDetailsConverter.convert(detailedArticle)
                    }
                }
            }
                .map { articles ->
                    articles.toImmutableList()
                }
                .onEach { newArticles ->
                    val currentState = _state.value
                    val newIndex = NewsDetailsIndexManager.calculateNewIndex(
                        currentState = NewsDetailsIndexManager.NewsDetailsState(
                            articles = currentState.articles,
                            selectedArticleIndex = currentState.selectedArticleIndex,
                        ),
                        newArticles = newArticles,
                        defaultArticleId = params.articleId,
                    )
                    stateFactory.updateArticles(newArticles, newIndex)
                }
                .launchIn(modelScope)
        }
    }

    private suspend fun initialPrefetch() {
        observeNewsDetailsUseCase.prefetch(
            newsIds = params.preselectedArticlesId,
            language = currentLanguage,
        ).onLeft { errors ->
            errors.onEach { (newsId, error) ->
                when {
                    // an article is opened from deeplink and is not found
                    params.screenSource == AnalyticsParam.ScreensSources.NewsLink.value &&
                        newsId == params.articleId &&
                        error is ApiResponseError.HttpException &&
                        error.code == ApiResponseError.HttpException.Code.NOT_FOUND
                    -> {
                        analyticsEventHandler.send(
                            NewsDetailsAnalyticsEvent.NewsLinkMismatch(
                                newsId = params.articleId,
                                code = error.code.numericCode,
                                message = error.message.orEmpty(),
                            ),
                        )
                    }
                    // global request executing error
                    newsId < 0 -> {
                        Timber.e(error)
                    }
                    else -> {
                        val (code, message) = when (error) {
                            is ApiResponseError.HttpException -> error.code.numericCode to error.message.orEmpty()
                            else -> null to ""
                        }
                        analyticsEventHandler.send(
                            NewsDetailsAnalyticsEvent.NewsArticleLoadError(
                                newsId = newsId,
                                code = code,
                                message = message,
                            ),
                        )
                    }
                }
            }
        }.mapLeft {
            stateFactory.createErrorState()
        }
    }

    private fun onRetryClicked() {
        stateFactory.createLoadingState()
        modelScope.launch(dispatchers.default) {
            initialPrefetch()
            paginationManager?.reload()
        }
    }

    private fun loadRelatedTokens(article: ArticleUM) {
        modelScope.launch(dispatchers.default) {
            stateFactory.updateRelatedTokens(RelatedTokensUM.Loading)
            val appCurrency = currentAppCurrency.value
            val resultState = relatedTokensLoader.load(
                articleId = article.id,
                relatedTokens = article.relatedTokens,
                appCurrency = appCurrency,
                onTokenClick = { tokenParams, currency ->
                    params.onTokenClick(tokenParams, currency)
                },
            )
            stateFactory.updateRelatedTokens(resultState)
        }
    }

    private fun markArticleAsViewed(articleId: Int) {
        modelScope.launch(dispatchers.default) {
            markArticleAsViewedUseCase.markAsViewed(articleId)
        }
    }

    companion object {
        internal const val RELATED_TOKEN_MAX_COUNT = 5
    }
}