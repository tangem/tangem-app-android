package com.tangem.features.feed.model.feed.state.transformers

import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
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

        val shouldBeContent = !isLoading && items.isNotEmpty() && error == null
        val isDataChanged = isDataChanged(
            prevChart = prevChart,
            isLoading = isLoading,
            shouldBeError = error != null,
            shouldBeContent = shouldBeContent,
            items = items,
        )

        if (!isDataChanged && prevChart != null) {
            return prevChart
        }

        return when {
            isLoading -> MarketChartUM.Loading
            error != null -> MarketChartUM.LoadingError(onRetryClicked = { onReload(sortByType) })
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
        checkAndSendErrorAnalytics(
            prevState = prevState,
            newMarketCharts = newMarketCharts,
            source = AnalyticsParam.ScreensSources.Markets,
            keyPredicate = { it == SortByTypeUM.Rating },
        )

        checkAndSendErrorAnalytics(
            prevState = prevState,
            newMarketCharts = newMarketCharts,
            source = AnalyticsParam.ScreensSources.MarketPulse,
            keyPredicate = { it != SortByTypeUM.Rating },
        )
    }

    private fun checkAndSendErrorAnalytics(
        prevState: FeedListUM,
        newMarketCharts: Map<SortByTypeUM, MarketChartUM>,
        source: AnalyticsParam.ScreensSources,
        keyPredicate: (SortByTypeUM) -> Boolean,
    ) {
        val wasError = prevState.marketChartConfig.marketCharts.filterKeys(keyPredicate)
            .values.any { it is MarketChartUM.LoadingError }

        val isErrorNow = newMarketCharts.filterKeys(keyPredicate)
            .values.any { it is MarketChartUM.LoadingError }

        if (!wasError && isErrorNow) {
            val relevantErrors = errorStatesByOrder.filterKeys(keyPredicate)
            val relevantItems = itemsByOrder.filterKeys(keyPredicate)

            val firstError = relevantErrors.values.firstNotNullOfOrNull { it }
            val isAnyListEmpty = relevantItems.values.any { it.isEmpty() }

            if (firstError == null && !isAnyListEmpty) {
                return
            }

            val (code, message) = if (firstError is ApiResponseError.HttpException) {
                firstError.code.numericCode to firstError.message.orEmpty()
            } else {
                null to (firstError?.message ?: "Empty response")
            }

            analyticsEventHandler.send(
                FeedAnalyticsEvent.MarketsLoadError(
                    code = code,
                    message = message,
                    source = source,
                ),
            )
        }
    }
}