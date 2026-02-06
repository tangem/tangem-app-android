package com.tangem.features.feed.model.news.details.loader

import arrow.core.getOrElse
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.*
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.news.RelatedToken
import com.tangem.features.feed.model.news.details.converter.RelatedTokenConverter
import com.tangem.features.feed.model.news.details.converter.TokenMarketInfoToParamsConverter
import com.tangem.features.feed.ui.news.details.state.RelatedTokensUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

internal class NewsRelatedTokensLoader(
    private val getTokenMarketInfoUseCase: GetTokenMarketInfoUseCase,
    private val getTokenPriceChartUseCase: GetTokenPriceChartUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
    private val maxCount: Int,
) {
    private val relatedTokensCache = mutableMapOf<Int, RelatedTokensUM>()
    private val tokenMarketInfoToParamsConverter = TokenMarketInfoToParamsConverter()

    /**
     * Loads data about related tokens.
     * Returns the cached result if available, otherwise loads the data.
     *
     * @param articleId article ID for caching
     * @param relatedTokens list of related tokens
     * @param appCurrency application currency
     * @param onTokenClick token click handler
     * @return state of related tokens
     */
    suspend fun load(
        articleId: Int,
        relatedTokens: List<RelatedToken>,
        appCurrency: AppCurrency,
        onTokenClick: (TokenMarketParams, AppCurrency) -> Unit,
    ): RelatedTokensUM = withContext(dispatchers.default) {
        val cached = getCachedRelatedTokens(articleId)
        if (cached != null) {
            return@withContext cached
        }

        val tokensToLoad = relatedTokens.take(maxCount)
        if (tokensToLoad.isEmpty()) {
            val errorState = RelatedTokensUM.LoadingError
            relatedTokensCache[articleId] = errorState
            return@withContext errorState
        }

        val tokenDataList = loadTokensData(tokensToLoad, appCurrency)

        if (tokenDataList.isEmpty()) {
            val errorState = RelatedTokensUM.LoadingError
            relatedTokensCache[articleId] = errorState
            return@withContext errorState
        }

        val resultState = createRelatedTokensState(tokenDataList, appCurrency, onTokenClick)
        relatedTokensCache[articleId] = resultState
        resultState
    }

    private fun getCachedRelatedTokens(articleId: Int): RelatedTokensUM? {
        return relatedTokensCache[articleId]?.takeIf { it !is RelatedTokensUM.Loading }
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
    ): TokenChart? {
        val chartResult = getTokenPriceChartUseCase(
            appCurrency = appCurrency,
            interval = PriceChangeInterval.H24,
            tokenId = tokenId,
            tokenSymbol = tokenSymbol,
            preview = true,
        )
        return chartResult.getOrElse { null }
    }

    private fun createRelatedTokensState(
        tokenDataList: List<Pair<MarketsListItemUM, TokenMarketParams>>,
        appCurrency: AppCurrency,
        onTokenClick: (TokenMarketParams, AppCurrency) -> Unit,
    ): RelatedTokensUM.Content {
        val tokenItems = tokenDataList.map { it.first }
        val onTokenClickHandler = createTokenClickHandler(tokenDataList, appCurrency, onTokenClick)

        return RelatedTokensUM.Content(
            items = tokenItems.toImmutableList(),
            onTokenClick = onTokenClickHandler,
        )
    }

    private fun createTokenClickHandler(
        tokenDataList: List<Pair<MarketsListItemUM, TokenMarketParams>>,
        appCurrency: AppCurrency,
        onTokenClick: (TokenMarketParams, AppCurrency) -> Unit,
    ): (MarketsListItemUM) -> Unit {
        return { item ->
            val tokenData = tokenDataList.find { it.first.id == item.id }
            tokenData?.second?.let { tokenParams ->
                onTokenClick(tokenParams, appCurrency)
            }
        }
    }
}