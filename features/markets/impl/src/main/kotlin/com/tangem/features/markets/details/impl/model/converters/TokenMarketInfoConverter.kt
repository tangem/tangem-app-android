package com.tangem.features.markets.details.impl.model.converters

import androidx.compose.runtime.Stable
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.markets.details.impl.ui.state.InfoBottomSheetContent
import com.tangem.features.markets.details.impl.ui.state.LinksUM
import com.tangem.features.markets.details.impl.ui.state.MarketsTokenDetailsUM
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

@Stable
internal class TokenMarketInfoConverter(
    appCurrency: Provider<AppCurrency>,
    onInfoClick: (InfoBottomSheetContent) -> Unit,
    onLinkClick: (LinksUM.Link) -> Unit,
) : Converter<TokenMarketInfo, MarketsTokenDetailsUM.InformationBlocks> {

    private val insightsConverter = InsightsConverter(appCurrency = appCurrency, onInfoClick = onInfoClick)
    private val securityScoreConverter = SecurityScoreConverter(onInfoClick = onInfoClick)
    private val metricsConverter = MetricsConverter(appCurrency = appCurrency, onInfoClick = onInfoClick)
    private val pricePerformanceConverter = PricePerformanceConverter(appCurrency = appCurrency)
    private val linksConverter = LinksConverter(onLinkClick = onLinkClick)

    override fun convert(value: TokenMarketInfo): MarketsTokenDetailsUM.InformationBlocks {
        return MarketsTokenDetailsUM.InformationBlocks(
            insights = value.insights?.let { insightsConverter.convert(it) },
            securityScore = securityScoreConverter.convert(Unit),
            metrics = value.metrics?.let { metricsConverter.convert(it) },
            pricePerformance = value.pricePerformance?.let { pricePerformanceConverter.convert(it) },
            links = value.links?.let { linksConverter.convert(it) },
        )
    }
}
