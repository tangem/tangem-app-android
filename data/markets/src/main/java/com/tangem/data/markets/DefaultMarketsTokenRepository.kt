package com.tangem.data.markets

import com.tangem.data.markets.converters.*
import com.tangem.data.markets.utils.retryOnError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.markets.TangemTechMarketsApi
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.markets.*
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.pagination.*
import com.tangem.pagination.fetcher.LimitOffsetBatchFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

internal class DefaultMarketsTokenRepository(
    private val marketsApi: TangemTechMarketsApi,
    private val tangemTechApi: TangemTechApi,
    private val dispatcherProvider: CoroutineDispatcherProvider,
) : MarketsTokenRepository {

    private val tokenListConverter = TokenMarketListConverter()
    private val tokenChartConverter = TokenChartConverter()

    private fun createTokenMarketsFetcher(firstBatchSize: Int, nextBatchSize: Int) = LimitOffsetBatchFetcher(
        prefetchDistance = firstBatchSize,
        batchSize = nextBatchSize,
        subFetcher = object : LimitOffsetBatchFetcher.SubFetcher<TokenMarketListConfig, List<TokenMarket>> {

            val requestTimeStamp = AtomicLong(0)

            override suspend fun fetch(
                request: LimitOffsetBatchFetcher.Request<TokenMarketListConfig>,
                lastResult: BatchFetchResult<List<TokenMarket>>?,
                isFirstBatchFetching: Boolean,
            ): BatchFetchResult<List<TokenMarket>> {
                val searchText =
                    if (request.params.searchText.isNullOrBlank()) null else request.params.searchText

                val requestCall = suspend {
                    marketsApi.getCoinsList(
                        currency = request.params.fiatPriceCurrency,
                        interval = request.params.priceChangeInterval.toRequestParam(),
                        order = request.params.order.toRequestParam(),
                        search = searchText,
                        offset = request.offset,
                        limit = request.limit,
                        timestamp = if (isFirstBatchFetching) null else requestTimeStamp.get(),
                    ).getOrThrow()
                }

                // we shouldn't infinitely retry on the first batch request
                val res = if (isFirstBatchFetching) {
                    requestCall()
                } else {
                    retryOnError(priority = true) {
                        requestCall()
                    }
                }

                if (isFirstBatchFetching) {
                    requestTimeStamp.set(res.timestamp ?: 0)
                }

                val last = res.tokens.size < request.limit

                return BatchFetchResult.Success(
                    data = tokenListConverter.convert(res),
                    last = last,
                    empty = res.tokens.isEmpty(),
                )
            }
        },
    )

    override fun getTokenListFlow(
        batchingContext: BatchingContext<Int, TokenMarketListConfig, TokenMarketUpdateRequest>,
        firstBatchSize: Int,
        nextBatchSize: Int,
    ): BatchFlow<Int, List<TokenMarket>, TokenMarketUpdateRequest> {
        val tokenMarketsUpdateFetcher = MarketsBatchUpdateFetcher(
            tangemTechApi = tangemTechApi,
            marketsApi = marketsApi,
        )

        val atomicInteger = AtomicInteger(0)

        return BatchListSource(
            fetchDispatcher = dispatcherProvider.io,
            context = batchingContext,
            generateNewKey = { atomicInteger.getAndIncrement() },
            batchFetcher = createTokenMarketsFetcher(firstBatchSize = firstBatchSize, nextBatchSize = nextBatchSize),
            updateFetcher = tokenMarketsUpdateFetcher,
        ).toBatchFlow()
    }

    override suspend fun getChart(
        fiatCurrencyCode: String,
        interval: PriceChangeInterval,
        tokenId: String,
    ): TokenChart {
        val response = marketsApi.getCoinChart(
            currency = fiatCurrencyCode,
            coinId = tokenId,
            interval = interval.toRequestParam(),
        )

        return tokenChartConverter.convert(interval, response.getOrThrow())
    }

    override suspend fun getTokenInfo(
        fiatCurrencyCode: String,
        tokenId: String,
        languageCode: String,
    ): TokenMarketInfo {
        val response = marketsApi.getCoinMarketData(
            currency = fiatCurrencyCode,
            coinId = tokenId,
            language = languageCode,
        )

        return TokenMarketInfoConverter().convert(response.getOrThrow())
    }
}