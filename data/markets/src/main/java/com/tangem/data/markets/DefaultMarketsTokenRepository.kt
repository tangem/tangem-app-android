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
import kotlinx.coroutines.*

internal class DefaultMarketsTokenRepository(
    private val marketsApi: TangemTechMarketsApi,
    private val tangemTechApi: TangemTechApi,
    private val dispatcherProvider: CoroutineDispatcherProvider,
) : MarketsTokenRepository {

    private val tokenListConverter = TokenMarketListConverter()

    private val tokenMarketsFetcher
        get() = LimitOffsetBatchFetcher(
            prefetchDistance = 150,
            batchSize = 100,
            subFetcher = object : LimitOffsetBatchFetcher.SubFetcher<TokenMarketListConfig, List<TokenMarket>> {

                var requestTimeStamp: Long? = null // TODO when backend is ready

                override suspend fun fetch(
                    request: LimitOffsetBatchFetcher.Request<TokenMarketListConfig>,
                    lastResult: BatchFetchResult<List<TokenMarket>>?,
                ): BatchFetchResult<List<TokenMarket>> {
                    val res = retryOnError(priority = true) {
                        marketsApi.getCoinsList(
                            currency = request.params.fiatPriceCurrency,
                            interval = request.params.priceChangeInterval.toRequestParam(),
                            order = request.params.order.toRequestParam(),
                            search = request.params.searchText,
                            generalCoins = request.params.showUnder100kMarketCapTokens.not(),
                            offset = request.offset,
                            limit = request.limit,
                        ).getOrThrow()
                    }

                    val last = res.tokens.size < request.limit

                    return BatchFetchResult.Success(
                        data = tokenListConverter.convert(res),
                        last = last,
                    )
                }
            },
        )

    override fun getTokenListFlow(
        batchingContext: BatchingContext<Int, TokenMarketListConfig, TokenMarketUpdateRequest>,
    ): BatchFlow<Int, List<TokenMarket>, TokenMarketUpdateRequest> {
        val tokenMarketsUpdateFetcher = MarketsBatchUpdateFetcher(
            tangemTechApi = tangemTechApi,
            marketsApi = marketsApi,
        )

        return BatchListSource(
            fetchDispatcher = dispatcherProvider.io,
            context = batchingContext,
            generateNewKey = { it.size },
            batchFetcher = tokenMarketsFetcher,
            updateFetcher = tokenMarketsUpdateFetcher,
        ).toBatchFlow()
    }
}