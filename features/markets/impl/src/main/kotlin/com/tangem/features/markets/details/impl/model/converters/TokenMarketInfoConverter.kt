package com.tangem.features.markets.details.impl.model.converters

import androidx.compose.runtime.Stable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.markets.details.impl.ui.state.LinksUM
import com.tangem.features.markets.details.impl.ui.state.ListedOnUM
import com.tangem.features.markets.details.impl.ui.state.MarketsTokenDetailsUM
import com.tangem.features.markets.details.impl.ui.state.SecurityScoreBottomSheetContent
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

@Stable
@Suppress("LongParameterList")
internal class TokenMarketInfoConverter(
    private val appCurrency: Provider<AppCurrency>,
    private val needApplyFCARestrictions: Provider<Boolean>,
    private val onInfoClick: (TangemBottomSheetConfigContent) -> Unit,
    private val onListedOnClick: (Int) -> Unit,
    onSecurityScoreInfoClick: (SecurityScoreBottomSheetContent) -> Unit,
    onLinkClick: (LinksUM.Link) -> Unit,
    onSecurityScoreProviderLinkClick: (SecurityScoreBottomSheetContent.SecurityScoreProviderUM) -> Unit,
    onPricePerformanceIntervalChanged: (PriceChangeInterval) -> Unit,
    onInsightsIntervalChanged: (PriceChangeInterval) -> Unit,
) : Converter<TokenMarketInfo, MarketsTokenDetailsUM.InformationBlocks> {

    private val insightsConverter = InsightsConverter(
        appCurrency = appCurrency,
        onInfoClick = onInfoClick,
        onIntervalChanged = onInsightsIntervalChanged,
    )

    private val securityScoreConverter = SecurityScoreConverter(
        onSecurityScoreInfoClick = onSecurityScoreInfoClick,
        onSecurityScoreProviderLinkClick = onSecurityScoreProviderLinkClick,
    )
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
        val insights = if (needApplyFCARestrictions()) {
            null
        } else {
            value.insights?.let { insightsConverter.convert(it) }
        }
        val securityScore = if (needApplyFCARestrictions()) {
            null
        } else {
            value.securityData?.let { securityScoreConverter.convert(it) }
        }
        return MarketsTokenDetailsUM.InformationBlocks(
            insights = insights,
            securityScore = securityScore,
            metrics = value.metrics?.let { metricsConverter.convert(it) },
            pricePerformance = value.pricePerformance?.let {
                pricePerformanceConverter.convert(
                    value = it,
                    currentPrice = value.quotes.currentPrice,
                )
            },
            listedOn = if (exchangesAmount != null && exchangesAmount > 0) {
                ListedOnUM.Content(onClick = { onListedOnClick(exchangesAmount) }, amount = exchangesAmount)
            } else {
                ListedOnUM.Empty
            },
            links = value.links?.let { linksConverter.convert(it) },
        )
    }
}