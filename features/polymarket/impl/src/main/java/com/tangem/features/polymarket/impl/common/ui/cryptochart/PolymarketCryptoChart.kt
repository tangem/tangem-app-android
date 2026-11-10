package com.tangem.features.polymarket.impl.common.ui.cryptochart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/**
 * Full price plot with price/time axes. Window selection, formatted copy and data sources belong to the caller.
 * [formatPrice] receives the minimum fraction digits needed to distinguish adjacent axis labels.
 * [targetLabel] contains the complete localized target caption, including its price.
 * [Figma](https://www.figma.com/design/dNvLAk1K7ZQ35oWN2aoAAY/Polymarket?node-id=11650-294787)
 */
@Composable
fun PolymarketCryptoChart(
    snapshot: PolymarketCryptoChart.Snapshot,
    targetLabel: TextReference,
    emptyLabel: TextReference,
    formatPrice: (Double, Int) -> String,
    formatTime: (Long) -> String,
    modifier: Modifier = Modifier,
) {
    CryptoPricePlot(
        snapshot = snapshot,
        targetLabel = targetLabel,
        emptyLabel = emptyLabel,
        formatPrice = formatPrice,
        formatTime = formatTime,
        isCompact = false,
        modifier = modifier,
    )
}

/**
 * Passive price preview for the whole caller-supplied interval, with no axes, gestures or running clock.
 * Uses the same target and price projection as the full chart.
 * [Figma](https://www.figma.com/design/dNvLAk1K7ZQ35oWN2aoAAY/Polymarket?node-id=11650-294731)
 */
@Composable
fun PolymarketCryptoChartMini(
    snapshot: PolymarketCryptoChart.Snapshot,
    targetLabel: TextReference,
    emptyLabel: TextReference,
    formatPrice: (Double, Int) -> String,
    modifier: Modifier = Modifier,
) {
    CryptoPricePlot(
        snapshot = snapshot,
        targetLabel = targetLabel,
        emptyLabel = emptyLabel,
        formatPrice = formatPrice,
        formatTime = { "" },
        isCompact = true,
        modifier = modifier,
    )
}

object PolymarketCryptoChart {

    /** Sorted, unique UTC timestamps. Prices are drawing values, never order/settlement amounts. */
    @Immutable
    data class Snapshot(val points: ImmutableList<Point>, val range: TimeRange, val targetPrice: Double?) {
        init {
            require(targetPrice == null || targetPrice.isFinite() && targetPrice >= 0)
            require(points.zipWithNext().all { (left, right) -> left.timestampMillis < right.timestampMillis })
        }
    }

    /** [isSegmentStart] prevents interpolation across a known gap; the renderer never infers a gap policy. */
    @Immutable
    data class Point(val timestampMillis: Long, val price: Double, val isSegmentStart: Boolean = false) {
        init {
            require(price.isFinite() && price >= 0)
        }
    }

    @Immutable
    data class TimeRange(val startMillis: Long, val endMillis: Long) {
        init {
            require(startMillis < endMillis)
        }
    }
}