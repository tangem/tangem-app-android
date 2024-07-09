package com.tangem.data.markets

import com.tangem.data.markets.converters.*
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.markets.TangemTechMarketsApi
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.markets.*
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.pagination.*
import com.tangem.pagination.fetcher.LimitOffsetBatchFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class DefaultMarketsTokenRepository(
    private val marketsApi: TangemTechMarketsApi,
    private val tangemTechApi: TangemTechApi,
    private val dispatcherProvider: CoroutineDispatcherProvider,
) : MarketsTokenRepository {

    private val tokenListConverter = TokenMarketListConverter()
    private val tokenListChartsConverter = TokenMarketChartsConverter(TokenListChartConverter())
    private val tokenQuotesConverter = TokenQuotesConverter()

    private val tokenMarketsFetcher
        get() = LimitOffsetBatchFetcher<TokenMarketListConfig, List<TokenMarket>>(
            prefetchDistance = 50,
            batchSize = 30,
            fetch = { params ->
                withContext(dispatcherProvider.io) {
                    val res = marketsApi.getCoinsList(
                        currency = params.request.fiatPriceCurrency,
                        interval = params.request.priceChangeInterval.toRequestParam(),
                        order = params.request.priceChangeInterval.toRequestParam(),
                        search = params.request.searchText,
                        generalCoins = params.request.showUnder100kMarketCapTokens.not(),
                        offset = params.offset,
                        limit = params.limit,
                    ).getOrThrow()

                    val last = res.tokens.size < params.limit

                    BatchFetchResult.Success(
                        data = tokenListConverter.convert(res),
                        last = last,
                    )
                }
            },
        )

    private val tokenMarketsUpdateFetcher
        get() = BatchUpdateFetcher<Int, List<TokenMarket>, TokenMarketUpdateRequest> { toUpdate, updateRequest ->
            withContext(dispatcherProvider.io) {
                val idsToUpdate = toUpdate.map { batch ->
                    batch.data.map { it.id }
                }.flatten()

                val updatedBatches = when (updateRequest) {
                    is TokenMarketUpdateRequest.UpdateChart -> {
                        val res = marketsApi.getCoinsListCharts(
                            coinIds = idsToUpdate,
                            interval = updateRequest.interval.toRequestParam(),
                            currency = updateRequest.currency,
                        ).getOrThrow()

                        toUpdate.map { batch ->
                            batch.copy(
                                data = batch.data.map {
                                    it.copy(
                                        tokenCharts = tokenListChartsConverter.convert(
                                            chartsToCopy = it.tokenCharts,
                                            tokenId = it.id,
                                            interval = updateRequest.interval,
                                            value = res,
                                        ),
                                    )
                                },
                            )
                        }
                    }
                    is TokenMarketUpdateRequest.UpdateQuotes -> {
                        val quotesRes = tangemTechApi.getQuotes(
                            currencyId = updateRequest.currencyId,
                            coinIds = idsToUpdate.joinToString(separator = ","),
                            fields = quoteFields.joinToString(separator = ","),
                        ).getOrThrow()

                        toUpdate.map { batch ->
                            batch.copy(
                                data = batch.data.map {
                                    it.copy(tokenQuotes = tokenQuotesConverter.convert(it.id, quotesRes))
                                },
                            )
                        }
                    }
                }

                BatchUpdateResult.Success(updatedBatches)
            }
        }

    override suspend fun getTokenListFlow(
        batchingContext: BatchingContext<Int, TokenMarketListConfig, TokenMarketUpdateRequest>,
    ): BatchFlow<Int, List<TokenMarket>, TokenMarketUpdateRequest> {
        return BatchListSource(
            fetchDispatcher = dispatcherProvider.io,
            context = batchingContext,
            generateNewKey = { it.size },
            batchFetcher = tokenMarketsFetcher,
            updateFetcher = tokenMarketsUpdateFetcher,
        ).toBatchFlow()
    }

    companion object {
        private val quoteFields = listOf(
            "price",
            "priceChange24h",
            "priceChange1w",
            "priceChange30d",
        )
    }
}
