package com.tangem.features.markets.details.impl.model.converters

import androidx.compose.runtime.Stable
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.markets.details.impl.ui.state.InfoBottomSheetContent
import com.tangem.features.markets.details.impl.ui.state.LinksUM
import com.tangem.features.markets.details.impl.ui.state.ListedOnUM
import com.tangem.features.markets.details.impl.ui.state.MarketsTokenDetailsUM
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

@Stable
internal class TokenMarketInfoConverter(
    private val appCurrency: Provider<AppCurrency>,
    private val onInfoClick: (InfoBottomSheetContent) -> Unit,
    private val onListedOnClick: () -> Unit,
    onLinkClick: (LinksUM.Link) -> Unit,
    onPricePerformanceIntervalChanged: (PriceChangeInterval) -> Unit,
    onInsightsIntervalChanged: (PriceChangeInterval) -> Unit,
) : Converter<TokenMarketInfo, MarketsTokenDetailsUM.InformationBlocks> {

    private val insightsConverter = InsightsConverter(
        appCurrency = appCurrency,
        onInfoClick = onInfoClick,
        onIntervalChanged = onInsightsIntervalChanged,
    )

    @Suppress("UnusedPrivateMember")
    // TODO second markets iteration
    private val securityScoreConverter = SecurityScoreConverter(onInfoClick = onInfoClick)
    private val pricePerformanceConverter = PricePerformanceConverter(
        appCurrency = appCurrency,
        onIntervalChanged = onPricePerformanceIntervalChanged,
    )
    private val linksConverter = LinksConverter(onLinkClick = onLinkClick)

    override fun convert(value: TokenMarketInfo): MarketsTokenDetailsUM.InformationBlocks {
        val metricsConverter = MetricsConverter(
            tokenSymbol = value.symbol,
            appCurrency = appCurrency,
            onInfoClick = onInfoClick,
        )

        val exchangesAmount = value.exchangesAmount
        return MarketsTokenDetailsUM.InformationBlocks(
            insights = value.insights?.let { insightsConverter.convert(it) },
            securityScore = null,
            metrics = value.metrics?.let { metricsConverter.convert(it) },
            pricePerformance = value.pricePerformance?.let {
                pricePerformanceConverter.convert(
                    value = it,
                    currentPrice = value.quotes.currentPrice,
                )
            },
            listedOn = if (exchangesAmount != null && exchangesAmount > 0) {
                ListedOnUM.Content(onClick = onListedOnClick, amount = exchangesAmount)
            } else {
                ListedOnUM.Empty
            },
            links = value.links?.let { linksConverter.convert(it) },
        )
    }
}