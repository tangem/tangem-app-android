package com.tangem.features.foryou.impl.model.converter.portfolioReview

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.components.state.AiInsightUM
import com.tangem.features.foryou.impl.components.state.DonutChartUM
import com.tangem.features.foryou.impl.components.state.DonutSegmentColor
import com.tangem.features.foryou.impl.components.state.DonutSegmentUM
import com.tangem.features.foryou.impl.components.state.MarketChartUM
import com.tangem.features.foryou.impl.model.converter.toForYouPercent
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

internal class ForYouPortfolioReviewMarketChartConverter(
    private val appCurrency: AppCurrency,
    private val topAssets: List<Pair<List<CryptoCurrencyStatus>, BigDecimal>>,
) : Converter<TotalFiatBalance?, MarketChartUM> {
    override fun convert(value: TotalFiatBalance?): MarketChartUM {
        val topBalance = topAssets.sumOf { (_, assetBalance) -> assetBalance }
        return when (value) {
            is TotalFiatBalance.Loaded -> MarketChartUM.Loaded(
                donutChart = DonutChartUM.Loaded(
                    totalAmount = value.amount.format {
                        fiat(
                            fiatCurrencySymbol = appCurrency.symbol,
                            fiatCurrencyCode = appCurrency.code,
                        )
                    },
                    donutSegmentList = topAssets.mapIndexed { index, (currencies, segmentBalance) ->
                        val segmentWeight = segmentBalance.toForYouPercent(value.amount).orZero()
                        DonutSegmentUM(
                            color = DonutSegmentColor.entries.getOrNull(index) ?: DonutSegmentColor.Brand,
                            weight = segmentWeight,
                            title = stringReference(currencies.firstOrNull()?.currency?.name.orEmpty()),
                            fiatValue = stringReference(segmentBalance.format {
                                fiat(
                                    fiatCurrencyCode = appCurrency.code,
                                    fiatCurrencySymbol = appCurrency.symbol,
                                )
                            }),
                        )
                    }.toPersistentList(),
                ),
                aiInsight = AiInsightUM.Hide,
                topHoldingPercent = stringReference(topBalance.toForYouPercent(value.amount).format { percent() }),
            )
            TotalFiatBalance.Loading,
            TotalFiatBalance.Failed,
            null,
            -> MarketChartUM.NoData(
                title = resourceReference(R.string.market_chart_can_not_load_data),
                donutText = resourceReference(R.string.market_chart_bubble_no_data),
            )
        }
    }
}