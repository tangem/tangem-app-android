package com.tangem.features.feed.ui.market.detailed.preview

import com.tangem.common.ui.charts.state.MarketChartDataProducer
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.features.feed.ui.market.detailed.state.*
import kotlinx.collections.immutable.persistentListOf

internal object MarketsTokenDetailsPreview {
    private val infoPoint = InfoPointUM(
        title = stringReference("1"),
        value = "2",
        change = InfoPointUM.ChangeType.DOWN,
        onInfoClick = {},
    )

    val loadingState = MarketsTokenDetailsUM(
        tokenName = "Token Name",
        priceText = "$0.00000000324",
        dateTimeText = stringReference("Today"),
        priceChangePercentText = "52.00%",
        iconUrl = "",
        priceChangeType = PriceChangeType.UP,
        chartState = MarketsTokenDetailsUM.ChartState(
            dataProducer = MarketChartDataProducer.build { },
            onLoadRetryClick = {},
            status = MarketsTokenDetailsUM.ChartState.Status.LOADING,
            onMarkerPointSelected = { _, _ -> },
        ),
        selectedInterval = PriceChangeInterval.H24,
        onSelectedIntervalChange = { },
        body = MarketsTokenDetailsUM.Body.Loading,
        bottomSheetConfig = TangemBottomSheetConfig(
            isShown = false,
            onDismissRequest = {},
            content = TangemBottomSheetConfigContent.Empty,
        ),
        isMarkerSet = false,
        triggerPriceChange = consumedEvent(),
        onShouldShowPriceSubtitleChange = {},
        shouldShowPriceSubtitle = false,
        relatedNews = MarketsTokenDetailsUM.RelatedNews(
            articles = persistentListOf(),
            onArticledClicked = {},
        ),
    )

    val contentState = MarketsTokenDetailsUM(
        tokenName = "Token Name",
        priceText = "$0.00000000324",
        dateTimeText = stringReference("Today"),
        priceChangePercentText = "52.00%",
        iconUrl = "",
        priceChangeType = PriceChangeType.UP,
        chartState = MarketsTokenDetailsUM.ChartState(
            dataProducer = MarketChartDataProducer.build { },
            onLoadRetryClick = {},
            status = MarketsTokenDetailsUM.ChartState.Status.LOADING,
            onMarkerPointSelected = { _, _ -> },
        ),
        selectedInterval = PriceChangeInterval.H24,
        onSelectedIntervalChange = { },
        body = MarketsTokenDetailsUM.Body.Content(
            description = MarketsTokenDetailsUM.Description(
                shortDescription = stringReference("markets_token_details_description_short"),
                fullDescription = stringReference("markets_token_details_description_full"),
                onReadMoreClick = {},
            ),
            infoBlocks = MarketsTokenDetailsUM.InformationBlocks(
                insights = InsightsUM(
                    h24Info = persistentListOf(
                        infoPoint,
                        infoPoint,
                        infoPoint,
                    ),
                    weekInfo = persistentListOf(
                        infoPoint,
                        infoPoint,
                        infoPoint,
                    ),
                    monthInfo = persistentListOf(
                        infoPoint,
                        infoPoint,
                        infoPoint,
                    ),
                    onInfoClick = {},
                    onIntervalChanged = {},
                ),
                securityScore = SecurityScoreUM(
                    score = 2.3f,
                    description = stringReference("markets_token_details_security_score_description"),
                    onInfoClick = {},
                ),
                metrics = MetricsUM(
                    metrics = persistentListOf(
                        infoPoint,
                        infoPoint,
                        infoPoint,
                    ),
                ),
                pricePerformance = PricePerformanceUM(
                    h24 = PricePerformanceUM.Value(
                        low = "1",
                        high = "2",
                        indicatorFraction = 0.3f,
                    ),
                    month = PricePerformanceUM.Value(
                        low = "1",
                        high = "2",
                        indicatorFraction = 0.3f,
                    ),
                    all = PricePerformanceUM.Value(
                        low = "1",
                        high = "2",
                        indicatorFraction = 0.3f,
                    ),
                    onIntervalChanged = {},
                ),
                listedOn = ListedOnUM.Empty,
                links = null,
            ),
        ),
        bottomSheetConfig = TangemBottomSheetConfig(
            isShown = false,
            onDismissRequest = {},
            content = TangemBottomSheetConfigContent.Empty,
        ),
        isMarkerSet = true,
        triggerPriceChange = consumedEvent(),
        onShouldShowPriceSubtitleChange = {},
        shouldShowPriceSubtitle = false,
        relatedNews = MarketsTokenDetailsUM.RelatedNews(
            articles = persistentListOf(),
            onArticledClicked = {},
        ),
    )
}