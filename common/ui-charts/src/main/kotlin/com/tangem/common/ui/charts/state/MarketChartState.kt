package com.tangem.common.ui.charts.state

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.core.cartesian.marker.LineCartesianLayerMarkerTarget
import java.math.BigDecimal

/**
 * MarketChartState used for MarketChart ui component.
 *
 * @param dataProducer The producer of the data for the Market Chart.
 * @param colorMapper A function that maps a MarketChartLook.Type to a Color.
 * @param onMarkerShown A callback function that is called when the marker is shown, hidden, or updated.
 * @return A MarketChartState.
 */
@Composable
fun rememberMarketChartState(
    dataProducer: MarketChartDataProducer = remember { MarketChartDataProducer.build {} },
    colorMapper: (MarketChartLook.Type) -> Color = remember {
        {
            when (it) {
                MarketChartLook.Type.Growing -> Color.Green
                MarketChartLook.Type.Falling -> Color.Red
            }
        }
    },
    onMarkerShown: (x: BigDecimal?, y: BigDecimal?) -> Unit = { _, _ -> },
): MarketChartState {
    val lookState = dataProducer.lookState.collectAsState()

    val state = remember(dataProducer, lookState, colorMapper, onMarkerShown) {
        MarketChartState(dataProducer, lookState, colorMapper, onMarkerShown)
    }

    return state
}

/**
 * Represents the state of a Market Chart.
 *
 * @property dataProducer The producer of the data for the Market Chart.
 * @property lookState The look state of the Market Chart.
 * @property colorMapper A function that maps a MarketChartLook.Type to a Color.
 * @property markerCallback A callback function that is called when the marker is shown, hidden, or updated.
 * @property isDrawingAnimationInProgress A boolean indicating whether the drawing animation is in progress.
 */
@Stable
class MarketChartState internal constructor(
    private val dataProducer: MarketChartDataProducer,
    private val lookState: State<MarketChartLook>,
    private val colorMapper: (MarketChartLook.Type) -> Color,
    private val markerCallback: (x: BigDecimal?, y: BigDecimal?) -> Unit,
) {
    internal val modelProducer = dataProducer.modelProducer

    internal val chartColor by derivedStateOf {
        colorMapper(lookState.value.type)
    }

    internal val markerHighlightRightSide by derivedStateOf {
        lookState.value.markerHighlightRightSide
    }

    internal val xValueFormatter = CartesianValueFormatter { value, _, _ ->
        val formatter = dataProducer.lookState.value.xAxisFormatter

        val state = dataProducer.dataState.value as? MarketChartData.Data
            ?: return@CartesianValueFormatter value.toString()

        formatter.format(
            value = dataProducer.pointsValuesConverter.prepareRawXForFormat(value, state),
        )
    }

    internal val yValueFormatter = CartesianValueFormatter { value, _, _ ->
        val formatter = dataProducer.lookState.value.yAxisFormatter

        val state = dataProducer.dataState.value as? MarketChartData.Data
            ?: return@CartesianValueFormatter value.toString()

        formatter.format(
            value = dataProducer.pointsValuesConverter.prepareRawYForFormat(value, state),
        )
    }

    internal var markerFraction by mutableStateOf<Float?>(null)

    internal val markerVisibilityListener = object : CartesianMarkerVisibilityListener {
        override fun onShown(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
            val point = getPoint(targets) ?: run {
                markerCallback(null, null)
                return
            }
            markerCallback(point.first, point.second)
        }

        override fun onHidden(marker: CartesianMarker) {
            markerCallback(null, null)
        }

        override fun onUpdated(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
            val point = getPoint(targets) ?: run {
                markerCallback(null, null)
                return
            }
            markerCallback(point.first, point.second)
        }
    }

    private fun getPoint(targets: List<CartesianMarker.Target>): Pair<BigDecimal, BigDecimal>? {
        val entry = (targets[0] as LineCartesianLayerMarkerTarget).points[0].entry
        val entryIndex = dataProducer.entries.value.indexOf(entry).takeIf { it != -1 } ?: return null
        val state = dataProducer.dataState.value as? MarketChartData.Data ?: return null
        val rawData = dataProducer.rawData.value ?: return null

        val originalIndex = rawData.originalIndexes?.getOrNull(entryIndex)
        val index = originalIndex ?: entryIndex

        val x = state.x.getOrNull(index) ?: return null
        val y = state.y.getOrNull(index) ?: return null
        return x to y
    }
}