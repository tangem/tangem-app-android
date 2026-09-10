package com.tangem.features.foryou.impl.model.converter.portfolioReview

import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.components.state.*
import com.tangem.features.foryou.impl.model.converter.toForYouPercent
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isPositive
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

/**
 * @property topAssets the assets shown individually, in rank order — one donut slice each.
 * @property otherAssetsBalance summed fiat balance of the assets collapsed into the "Other" row, or zero
 * when nothing was collapsed. Non-zero means the donut closes its ring with a grey "Other" slice.
 */
internal class ForYouPortfolioReviewMarketChartConverter(
    private val appCurrency: AppCurrency,
    private val topAssets: List<Pair<List<CryptoCurrencyStatus>, BigDecimal>>,
    private val otherAssetsBalance: BigDecimal,
    private val onSegmentTap: () -> Unit,
    private val isBalanceHidden: Boolean = false,
) : Converter<TotalFiatBalance?, MarketChartUM> {
    override fun convert(value: TotalFiatBalance?): MarketChartUM {
        val topBalance = topAssets.sumOf { (_, assetBalance) -> assetBalance }
        return when (value) {
            is TotalFiatBalance.Loaded -> MarketChartUM.Loaded(
                donutChart = DonutChartUM.Loaded(
                    totalAmount = value.amount.toFiat(),
                    donutSegmentList = createSegments(totalAmount = value.amount),
                    onSegmentTap = onSegmentTap,
                ),
                assetCount = topAssets.size,
                aiInsight = AiInsightUM.Hide,
                topHoldingPercent = resourceReference(
                    id = R.string.market_chart_top_holding,
                    formatArgs = wrappedList(topBalance.toForYouPercent(value.amount).toTopHoldingText()),
                ),
            )
            TotalFiatBalance.Loading,
            TotalFiatBalance.Failed,
            null,
            -> MarketChartUM.NoData(
                title = resourceReference(R.string.market_chart_can_not_load_data),
                donutText = resourceReference(R.string.markets_loading_no_data_title),
            )
        }
    }

    /**
     * The top assets in rank order, followed — once assets have been collapsed into "Other" — by a grey
     * slice standing for that collapsed remainder, so it is selectable and carries a tooltip like any
     * other slice.
     *
     * Its weight is the **exact complement** of the top slices, not `otherAssetsBalance / totalAmount`:
     * that closes the ring precisely, which stops `visualSweepAngles` reserving its minimum-share grey gap
     * on top of the slice over a fraction of a degree of rounding drift.
     */
    private fun createSegments(totalAmount: BigDecimal): ImmutableList<DonutSegmentUM> {
        val topSegments = topAssets.mapIndexed { index, (currencies, segmentBalance) ->
            DonutSegmentUM(
                color = DonutSegmentColor.entries.getOrNull(index) ?: DonutSegmentColor.Blue,
                weight = segmentBalance.toForYouPercent(totalAmount).orZero(),
                title = stringReference(currencies.firstOrNull()?.currency?.symbol.orEmpty()),
                fiatValue = stringReference(segmentBalance.toFiat()),
            )
        }

        val otherWeight = BigDecimal.ONE - topSegments.sumOf { it.weight }
        if (!otherAssetsBalance.isPositive() || !otherWeight.isPositive()) return topSegments.toPersistentList()

        val otherSegment = DonutSegmentUM(
            color = DonutSegmentColor.Grey,
            weight = otherWeight,
            title = resourceReference(R.string.common_other),
            fiatValue = stringReference(otherAssetsBalance.toFiat()),
        )
        return (topSegments + otherSegment).toPersistentList()
    }

    /**
     * The top assets' share of the portfolio, prefixed with [StringsSigns.TILDE_SIGN] when the rendering would
     * otherwise present an incomplete portfolio as a whole one: assets were collapsed into "Other", so the top
     * assets are mathematically below 100%, yet the displayed precision rounds up to exactly it.
     */
    private fun BigDecimal?.toTopHoldingText(): String {
        val formatted = format { percent(canBeLower = true) }
        val isRenderedAsWholePortfolio = formatted == BigDecimal.ONE.format { percent() }

        return if (otherAssetsBalance.isPositive() && isRenderedAsWholePortfolio) {
            StringsSigns.TILDE_SIGN + formatted
        } else {
            formatted
        }
    }

    private fun BigDecimal.toFiat(): String = format {
        fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
    }.orMaskWithStars(isBalanceHidden)
}