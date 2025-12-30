package com.tangem.features.feed.model.news.details

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetTokenMarketInfoUseCase
import com.tangem.domain.markets.GetTokenPriceChartUseCase
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.news.RelatedToken
import com.tangem.domain.news.usecase.MarkArticleAsViewedUseCase
import com.tangem.domain.news.usecase.ObserveNewsDetailsUseCase
import com.tangem.features.feed.components.news.details.DefaultNewsDetailsComponent
import com.tangem.features.feed.model.news.details.converter.NewsDetailsConverter
import com.tangem.features.feed.model.news.details.converter.RelatedTokenConverter
import com.tangem.features.feed.model.news.details.converter.TokenMarketInfoToParamsConverter
import com.tangem.features.feed.model.news.details.factory.NewsDetailsStateFactory
import com.tangem.features.feed.ui.news.details.state.ArticleUM
import com.tangem.features.feed.ui.news.details.state.NewsDetailsUM
import com.tangem.features.feed.ui.news.details.state.RelatedTokensUM
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.indexOfFirstOrNull
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class NewsDetailsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val observeNewsDetailsUseCase: ObserveNewsDetailsUseCase,
    private val urlOpener: UrlOpener,
    private val shareManager: ShareManager,
    private val getTokenMarketInfoUseCase: GetTokenMarketInfoUseCase,
    private val getTokenPriceChartUseCase: GetTokenPriceChartUseCase,
    private val markArticleAsViewedUseCase: MarkArticleAsViewedUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<DefaultNewsDetailsComponent.Params>()
    private val currentLanguage = Locale.getDefault().language
    private val relatedTokensCache = mutableMapOf<Int, RelatedTokensUM>()

    private val newsDetailsConverter = NewsDetailsConverter(onSourceClick = urlOpener::openUrl)
    private val tokenMarketInfoToParamsConverter = TokenMarketInfoToParamsConverter()

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
            articles = persistentListOf(),
            selectedArticleIndex = 0,
            onShareClick = {},
            onLikeClick = { /* [REDACTED_TODO_COMMENT] */ },
            onBackClick = params.onBackClicked,
            onArticleIndexChanged = ::onArticleIndexChanged,
        ),
    )

    private val stateFactory by lazy(LazyThreadSafetyMode.NONE) {
        NewsDetailsStateFactory(
            currentStateProvider = Provider { _state.value },
            shareManager = shareManager,
            onStateUpdate = { newState -> _state.update { newState } },
        )
    }

    val state: StateFlow<NewsDetailsUM> = _state.asStateFlow()

    init {
        if (params.preselectedArticlesId.isNotEmpty()) {
            handlePreselectedArticles()
        } else {
            // TODO handle pagination [REDACTED_TASK_KEY]
        }
    }

    private fun onArticleIndexChanged(newIndex: Int) {
        stateFactory.updateSelectedArticleIndex(newIndex)
        val currentArticle = state.value.articles.getOrNull(newIndex)
        currentArticle?.let { article ->
            markArticleAsViewed(article.id)
            loadRelatedTokens(article)
        }
    }

    private fun handlePreselectedArticles() {
        modelScope.launch {
            observeNewsDetailsUseCase.prefetch(
                newsIds = params.preselectedArticlesId,
                language = currentLanguage,
            )

            observeNewsDetailsUseCase
                .invoke()
                .map { articlesMap ->
                    params.preselectedArticlesId.mapNotNull { articleId ->
                        articlesMap[articleId]?.let { detailedArticle ->
                            newsDetailsConverter.convert(detailedArticle)
                        }
                    }
                }
                .map { articles ->
                    articles.toImmutableList()
                }
                .onEach { articles ->
                    val selectedIndex = articles.indexOfFirstOrNull { it.id == params.articleId } ?: 0
                    stateFactory.updateArticles(articles, selectedIndex)
                }
                .launchIn(modelScope)
        }
    }

    private fun loadRelatedTokens(article: ArticleUM) {
        modelScope.launch(dispatchers.default) {
            val cachedTokens = getCachedRelatedTokens(article.id)
            if (cachedTokens != null) {
                stateFactory.updateRelatedTokens(cachedTokens)
                return@launch
            }

            stateFactory.updateRelatedTokens(RelatedTokensUM.Loading)

            val relatedTokens = article.relatedTokens.take(RELATED_TOKEN_MAX_COUNT)
            if (relatedTokens.isEmpty()) {
                handleEmptyRelatedTokens(article.id)
                return@launch
            }

            val appCurrency = currentAppCurrency.value
            val tokenDataList = loadTokensData(relatedTokens, appCurrency)

            if (tokenDataList.isEmpty()) {
                handleEmptyTokenData(article.id)
                return@launch
            }

            val resultState = createRelatedTokensState(tokenDataList, appCurrency)
            relatedTokensCache[article.id] = resultState
            stateFactory.updateRelatedTokens(resultState)
        }
    }

    private fun getCachedRelatedTokens(articleId: Int): RelatedTokensUM? {
        return relatedTokensCache[articleId]?.takeIf { it !is RelatedTokensUM.Loading }
    }

    private fun handleEmptyRelatedTokens(articleId: Int) {
        val errorState = RelatedTokensUM.LoadingError
        relatedTokensCache[articleId] = errorState
        stateFactory.updateRelatedTokens(errorState)
    }

    private suspend fun CoroutineScope.loadTokensData(
        relatedTokens: List<RelatedToken>,
        appCurrency: AppCurrency,
    ): List<Pair<MarketsListItemUM, TokenMarketParams>> {
        val relatedTokenConverter = RelatedTokenConverter(appCurrency = appCurrency)

        return relatedTokens.map { token ->
            async(dispatchers.default) {
                loadSingleTokenData(token, appCurrency, relatedTokenConverter)
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun loadSingleTokenData(
        token: RelatedToken,
        appCurrency: AppCurrency,
        relatedTokenConverter: RelatedTokenConverter,
    ): Pair<MarketsListItemUM, TokenMarketParams>? {
        val tokenId = CryptoCurrency.RawID(token.id)
        val tokenInfoResult = getTokenMarketInfoUseCase(
            appCurrency = appCurrency,
            tokenId = tokenId,
            tokenSymbol = token.symbol,
        )

        return tokenInfoResult.fold(
            ifLeft = { null },
            ifRight = { tokenInfo ->
                val chart = loadTokenChart(tokenId, token.symbol, appCurrency)
                val tokenItem = relatedTokenConverter.convert(tokenInfo to chart)
                val tokenParams = tokenMarketInfoToParamsConverter.convert(tokenInfo)
                tokenItem to tokenParams
            },
        )
    }

    private suspend fun loadTokenChart(
        tokenId: CryptoCurrency.RawID,
        tokenSymbol: String,
        appCurrency: AppCurrency,
    ): com.tangem.domain.markets.TokenChart? {
        val chartResult = getTokenPriceChartUseCase(
            appCurrency = appCurrency,
            interval = PriceChangeInterval.H24,
            tokenId = tokenId,
            tokenSymbol = tokenSymbol,
            preview = true,
        )
        return chartResult.getOrElse { null }
    }

    private fun handleEmptyTokenData(articleId: Int) {
        val errorState = RelatedTokensUM.LoadingError
        relatedTokensCache[articleId] = errorState
        stateFactory.updateRelatedTokens(errorState)
    }

    private fun createRelatedTokensState(
        tokenDataList: List<Pair<MarketsListItemUM, TokenMarketParams>>,
        appCurrency: AppCurrency,
    ): RelatedTokensUM.Content {
        val tokenItems = tokenDataList.map { it.first }
        val onTokenClick = createTokenClickHandler(tokenDataList, appCurrency)

        return stateFactory.createRelatedTokensContent(
            items = tokenItems.toImmutableList(),
            onTokenClick = onTokenClick,
        )
    }

    private fun createTokenClickHandler(
        tokenDataList: List<Pair<MarketsListItemUM, TokenMarketParams>>,
        appCurrency: AppCurrency,
    ): (MarketsListItemUM) -> Unit {
        return { item ->
            val tokenData = tokenDataList.find { it.first.id == item.id }
            tokenData?.second?.let { tokenParams ->
                params.onTokenClick(tokenParams, appCurrency)
            }
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