package com.tangem.features.feed.model.feed.state.transformers

import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.features.feed.model.feed.analytics.FeedAnalyticsEvent
import com.tangem.features.feed.model.market.list.state.SortByTypeUM
import com.tangem.features.feed.ui.feed.state.FeedListUM
import com.tangem.features.feed.ui.feed.state.MarketChartUM
import com.tangem.features.feed.ui.feed.state.SortChartConfigUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentHashMap

internal class UpdateMarketChartsTransformer(
    private val itemsByOrder: Map<SortByTypeUM, ImmutableList<MarketsListItemUM>>,
    private val loadingStatesByOrder: Map<SortByTypeUM, Boolean>,
    private val errorStatesByOrder: Map<SortByTypeUM, Throwable?>,
    private val onReload: (SortByTypeUM) -> Unit,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : FeedListUMTransformer {

    override fun transform(prevState: FeedListUM): FeedListUM {
        val newMarketCharts = buildMap {
            SortByTypeUM.entries.forEach { sortByType ->
                val items = itemsByOrder[sortByType] ?: persistentListOf()
                val isLoading = loadingStatesByOrder[sortByType] == true
                val hasError = errorStatesByOrder[sortByType] != null

                when {
                    hasError -> {
                        put(
                            key = sortByType,
                            value = MarketChartUM.LoadingError(
                                onRetryClicked = { onReload(sortByType) },
                            ),
                        )
                    }
                    isLoading -> {
                        put(key = sortByType, value = MarketChartUM.Loading)
                    }
                    items.isEmpty() -> {
                        put(
                            key = sortByType,
                            value = MarketChartUM.LoadingError(
                                onRetryClicked = { onReload(sortByType) },
                            ),
                        )
                    }
                    else -> {
                        put(
                            key = sortByType,
                            value = MarketChartUM.Content(
                                items = items,
                                sortChartConfig = SortChartConfigUM(
                                    sortByType = sortByType,
                                    isSelected = sortByType == prevState.marketChartConfig.currentSortByType,
                                ),
                            ),
                        )
                    }
                }
            }
        }.toPersistentHashMap()

        val wasAnyErrorBefore = prevState.marketChartConfig.marketCharts.values.any {
            it is MarketChartUM.LoadingError
        }
        val isAnyErrorNow = newMarketCharts.values.any { it is MarketChartUM.LoadingError }

        if (!wasAnyErrorBefore && isAnyErrorNow) {
            sendErrorAnalytics(errorStatesByOrder, itemsByOrder)
        }

        return prevState.copy(
            marketChartConfig = prevState.marketChartConfig.copy(
                marketCharts = newMarketCharts,
            ),
        )
    }

    private fun sendErrorAnalytics(
        errorStatesByOrder: Map<SortByTypeUM, Throwable?>,
        itemsByOrder: Map<SortByTypeUM, ImmutableList<MarketsListItemUM>>,
    ) {
        val firstError = errorStatesByOrder.values.firstNotNullOfOrNull { it }
        if (firstError == null && !itemsByOrder.values.any { it.isEmpty() }) {
            return
        }
        val (code, message) = if (firstError is ApiResponseError.HttpException) {
            firstError.code.numericCode to firstError.message.orEmpty()
        } else {
            null to ""
        }
        analyticsEventHandler.send(
            FeedAnalyticsEvent.MarketsLoadError(
                code = code,
                message = message,
            ),
        )
    }
}