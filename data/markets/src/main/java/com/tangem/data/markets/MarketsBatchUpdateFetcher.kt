package com.tangem.data.markets

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.common.utils.retryOnError
import com.tangem.data.markets.analytics.MarketsDataAnalyticsEvent
import com.tangem.data.markets.converters.TokenMarketChartsConverter
import com.tangem.data.markets.converters.TokenQuotesShortConverter
import com.tangem.data.markets.converters.toRequestParam
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.markets.TangemTechMarketsApi
import com.tangem.datasource.api.markets.models.response.TokenMarketChartListResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.TangemTechApi.Companion.marketsQuoteFields
import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.markets.TokenMarketUpdateRequest
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchUpdateFetcher
import com.tangem.pagination.BatchUpdateResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal class MarketsBatchUpdateFetcher(
    private val marketsApi: TangemTechMarketsApi,
    private val tangemTechApi: TangemTechApi,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val onApiError: (ApiResponseError) -> Unit,
) : BatchUpdateFetcher<Int, List<TokenMarket>, TokenMarketUpdateRequest> {

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
                            catchApiError(onApiError) {
                                marketsApi.getCoinsListCharts(
                                    coinIds = batchIds.second.joinToString(separator = ","),
                                    interval = updateRequest.interval.toRequestParam(),
                                    currency = updateRequest.currency,
                                ).getOrThrow()
                            }
                        }
                    }
                }

                updateTasks.forEachIndexed { index, deferred ->
                    launch {
                        val res = deferred.await()
                        checkForNulls(res)
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
                    catchApiError(onApiError) {
                        tangemTechApi.getQuotes(
                            currencyId = updateRequest.currencyId,
                            coinIds = idsToUpdate.map { it.second }.flatten().joinToString(separator = ","),
                            fields = marketsQuoteFields.joinToString(separator = ","),
                        ).getOrThrow()
                    }
                }

                update {
                    val res = toUpdate.map { batch ->
                        batch.copy(
                            data = batch.data.map {
                                it.copy(tokenQuotesShort = TokenQuotesShortConverter.convert(it.id, quotesRes))
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
                    tokenCharts = TokenMarketChartsConverter.convert(
                        chartsToCopy = it.tokenCharts,
                        tokenId = it.id,
                        interval = updateRequest.interval,
                        value = update,
                    ),
                )
            },
        )
    }

    private fun checkForNulls(response: TokenMarketChartListResponse) {
        response.values.forEach { chart ->
            chart.prices.forEach { (_, price) ->
                if (price == null) {
                    analyticsEventHandler.send(
                        MarketsDataAnalyticsEvent.ChartNullValuesError(
                            requestPath = "coins/history_preview",
                            errorType = MarketsDataAnalyticsEvent.Type.Custom,
                        ),
                    )

                    return
                }
            }
        }
    }

    private inline fun <T> catchApiError(onError: (ApiResponseError) -> Unit, block: () -> T): T {
        return try {
            block()
        } catch (e: ApiResponseError) {
            onError(e)
            throw e
        }
    }
}