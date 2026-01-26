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
import kotlinx.collections.immutable.toPersistentMap

internal class UpdateMarketChartsTransformer(
    private val itemsByOrder: Map<SortByTypeUM, ImmutableList<MarketsListItemUM>>,
    private val loadingStatesByOrder: Map<SortByTypeUM, Boolean>,
    private val errorStatesByOrder: Map<SortByTypeUM, Throwable?>,
    private val onReload: (SortByTypeUM) -> Unit,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : FeedListUMTransformer {

    override fun transform(prevState: FeedListUM): FeedListUM {
        val newMarketCharts = SortByTypeUM.entries
            .associateWith { sortByType ->
                getNewMarketChart(
                    sortByType = sortByType,
                    prevChart = prevState.marketChartConfig.marketCharts[sortByType],
                )
            }
            .toPersistentMap()

        handleErrorAnalytics(prevState, newMarketCharts)

        return prevState.copy(
            marketChartConfig = prevState.marketChartConfig.copy(
                marketCharts = newMarketCharts,
            ),
        )
    }

    private fun getNewMarketChart(sortByType: SortByTypeUM, prevChart: MarketChartUM?): MarketChartUM {
        val items = itemsByOrder[sortByType] ?: persistentListOf()
        val isLoading = loadingStatesByOrder[sortByType] == true
        val error = errorStatesByOrder[sortByType]

        val shouldBeError = !isLoading && (items.isEmpty() || error != null)
        val shouldBeContent = !isLoading && items.isNotEmpty() && error == null
        val isDataChanged = isDataChanged(
            prevChart = prevChart,
            isLoading = isLoading,
            shouldBeError = shouldBeError,
            shouldBeContent = shouldBeContent,
            items = items,
        )

        if (!isDataChanged && prevChart != null) {
            return prevChart
        }

        return when {
            isLoading -> MarketChartUM.Loading
            shouldBeError -> MarketChartUM.LoadingError(onRetryClicked = { onReload(sortByType) })
            else -> createContentChart(prevChart, items, sortByType)
        }
    }

    private fun isDataChanged(
        prevChart: MarketChartUM?,
        isLoading: Boolean,
        shouldBeError: Boolean,
        shouldBeContent: Boolean,
        items: ImmutableList<MarketsListItemUM>,
    ): Boolean {
        return when (prevChart) {
            is MarketChartUM.Loading -> !isLoading
            is MarketChartUM.LoadingError -> !shouldBeError
            is MarketChartUM.Content -> !shouldBeContent || prevChart.items != items
            null -> true
        }
    }

    private fun createContentChart(
        prevChart: MarketChartUM?,
        items: ImmutableList<MarketsListItemUM>,
        sortByType: SortByTypeUM,
    ): MarketChartUM.Content {
        val prevContent = prevChart as? MarketChartUM.Content
        return MarketChartUM.Content(
            items = items,
            sortChartConfig = prevContent?.sortChartConfig
                ?: SortChartConfigUM(
                    sortByType = sortByType,
                    isSelected = false,
                ),
        )
    }

    private fun handleErrorAnalytics(prevState: FeedListUM, newMarketCharts: Map<SortByTypeUM, MarketChartUM>) {
        val wasAnyErrorBefore = prevState.marketChartConfig.marketCharts.values.any { it is MarketChartUM.LoadingError }
        val isAnyErrorNow = newMarketCharts.values.any { it is MarketChartUM.LoadingError }

        if (!wasAnyErrorBefore && isAnyErrorNow) {
            sendErrorAnalytics(errorStatesByOrder, itemsByOrder)
        }
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