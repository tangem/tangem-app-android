package com.tangem.data.markets

import com.tangem.data.markets.converters.TokenListChartConverter
import com.tangem.data.markets.converters.TokenMarketChartsConverter
import com.tangem.data.markets.converters.TokenQuotesConverter
import com.tangem.data.markets.converters.toRequestParam
import com.tangem.data.markets.utils.retryOnError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.markets.TangemTechMarketsApi
import com.tangem.datasource.api.markets.models.response.TokenMarketChartListResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.markets.TokenMarketUpdateRequest
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchUpdateFetcher
import com.tangem.pagination.BatchUpdateResult
import kotlinx.coroutines.*

internal class MarketsBatchUpdateFetcher(
    private val marketsApi: TangemTechMarketsApi,
    private val tangemTechApi: TangemTechApi,
) : BatchUpdateFetcher<Int, List<TokenMarket>, TokenMarketUpdateRequest> {

    private val tokenListChartsConverter = TokenMarketChartsConverter(TokenListChartConverter())
    private val tokenQuotesConverter = TokenQuotesConverter()

    override suspend fun BatchUpdateFetcher.UpdateContext<Int, List<TokenMarket>>.fetchUpdateAsync(
        toUpdate: List<Batch<Int, List<TokenMarket>>>,
        updateRequest: TokenMarketUpdateRequest,
    ) {
        val idsToUpdate = toUpdate.map { batch ->
            batch.key to batch.data.map { it.id }
        }

        when (updateRequest) {
            is TokenMarketUpdateRequest.UpdateChart -> coroutineScope {
                val updateTasks = idsToUpdate.map { batchIds ->
                    async {
                        retryOnError {
                            marketsApi.getCoinsListCharts(
                                coinIds = batchIds.second.joinToString(separator = ","),
                                interval = updateRequest.interval.toRequestParam(),
                                currency = updateRequest.currency,
                            ).getOrThrow()
                        }
                    }
                }

                updateTasks.forEachIndexed { index, deferred ->
                    launch {
                        val res = deferred.await()
                        val batchToUpdate = toUpdate[index]

                        update {
                            val resBatch = changeChartsInBatches(
                                updateRequest = updateRequest,
                                batchToUpdate = batchToUpdate,
                                update = res,
                            )
                            BatchUpdateResult.Success(resBatch)
                        }
                    }
                }
            }
            is TokenMarketUpdateRequest.UpdateQuotes -> {
                val quotesRes = retryOnError {
                    tangemTechApi.getQuotes(
                        currencyId = updateRequest.currencyId,
                        coinIds = idsToUpdate.map { it.second }.flatten().joinToString(separator = ","),
                        fields = quoteFields.joinToString(separator = ","),
                    ).getOrThrow()
                }

                update {
                    val res = toUpdate.map { batch ->
                        batch.copy(
                            data = batch.data.map {
                                it.copy(tokenQuotes = tokenQuotesConverter.convert(it.id, quotesRes))
                            },
                        )
                    }

                    BatchUpdateResult.Success(res)
                }
            }
        }
    }

    private fun List<Batch<Int, List<TokenMarket>>>.changeChartsInBatches(
        updateRequest: TokenMarketUpdateRequest.UpdateChart,
        batchToUpdate: Batch<Int, List<TokenMarket>>,
        update: TokenMarketChartListResponse,
    ): List<Batch<Int, List<TokenMarket>>> = mapNotNull { resultBatch ->
        if (batchToUpdate.key != resultBatch.key) return@mapNotNull null

        Batch(
            key = batchToUpdate.key,
            data = batchToUpdate.data.map {
                it.copy(
                    tokenCharts = tokenListChartsConverter.convert(
                        chartsToCopy = it.tokenCharts,
                        tokenId = it.id,
                        interval = updateRequest.interval,
                        value = update,
                    ),
                )
            },
        )
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