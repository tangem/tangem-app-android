package com.tangem.features.polymarket.impl.common.ui.cryptochart

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.polymarket.impl.common.ui.cryptochart.PolymarketCryptoChart.Point
import com.tangem.features.polymarket.impl.common.ui.cryptochart.PolymarketCryptoChart.Snapshot
import com.tangem.features.polymarket.impl.common.ui.cryptochart.PolymarketCryptoChart.TimeRange
import com.tangem.utils.time.CurrentTimeProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * Full chart with foreground-only motion and touch inspection. [timeRange] is the caller's window policy;
 * its argument already includes [renderDelayMillis] and is capped at [freezeAtMillis]. No window duration is assumed.
 * Change [windowId] when switching events/windows to discard the old animation and selection.
 * Rendering never subscribes to a price source or derives an event's result.
 */
@Composable
@Suppress("LongParameterList")
fun PolymarketLiveCryptoChart(
    points: ImmutableList<Point>,
    targetPrice: Double?,
    windowId: String,
    timeRange: (Long) -> TimeRange,
    timeProvider: CurrentTimeProvider,
    targetLabel: TextReference,
    emptyLabel: TextReference,
    formatPrice: (Double, Int) -> String,
    formatTime: (Long) -> String,
    modifier: Modifier = Modifier,
    freezeAtMillis: Long? = null,
    renderDelayMillis: Long = DEFAULT_RENDER_DELAY_MILLIS,
) {
    require(renderDelayMillis >= 0)
    key(windowId) {
        val snapshot = remember(points, targetPrice, timeRange, timeProvider, freezeAtMillis, renderDelayMillis) {
            val time = cryptoRenderTime(timeProvider.nowMillis(), renderDelayMillis, freezeAtMillis)
            Snapshot(points, timeRange(time), targetPrice)
        }
        val frame = rememberCryptoChartFrame(
            snapshot = snapshot,
            timeRange = timeRange,
            timeProvider = timeProvider,
            freezeAtMillis = freezeAtMillis,
            renderDelayMillis = renderDelayMillis,
        )
        CryptoPricePlot(
            snapshot = snapshot,
            targetLabel = targetLabel,
            emptyLabel = emptyLabel,
            formatPrice = formatPrice,
            formatTime = formatTime,
            isCompact = false,
            modifier = modifier,
            liveFrame = frame,
        )
    }
}

@Composable
private fun rememberCryptoChartFrame(
    snapshot: Snapshot,
    timeRange: (Long) -> TimeRange,
    timeProvider: CurrentTimeProvider,
    freezeAtMillis: Long?,
    renderDelayMillis: Long,
): State<CryptoChartFrame> {
    val frame = remember { mutableStateOf(
        CryptoChartFrame.from(snapshot.points, snapshot.range, snapshot.targetPrice),
    ) }
    val latestSnapshot = rememberUpdatedState(snapshot)
    val latestRange = rememberUpdatedState(timeRange)
    val latestFreeze = rememberUpdatedState(freezeAtMillis)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, timeProvider, renderDelayMillis) {
        val durationScale = currentCoroutineContext()[MotionDurationScale]
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val animator = CryptoChartAnimator()
            snapshotFlow { Triple(
                latestSnapshot.value,
                latestRange.value,
                latestFreeze.value,
            ) }.collectLatest { input ->
                val (data, range, freeze) = input
                var frozenSince: Long? = null
                do {
                    val frameMillis = withFrameNanos { it / NANOS_PER_MILLISECOND }
                    val renderedTime = cryptoRenderTime(timeProvider.nowMillis(), renderDelayMillis, freeze)
                    val scale = durationScale?.scaleFactor ?: 1f
                    val isFrozen = freeze != null && renderedTime >= freeze
                    if (isFrozen && frozenSince == null) frozenSince = frameMillis
                    val isSettled = isFrozen &&
                        frameMillis - requireNotNull(frozenSince) >= FREEZE_SETTLE_MILLIS * scale
                    frame.value = animator.frame(
                        points = data.points,
                        range = range(renderedTime),
                        target = data.targetPrice,
                        frameMillis = frameMillis,
                        motionScale = if (isSettled) 0f else scale,
                    )
                    if (isSettled) break
                    if (scale == 0f) delay(REDUCED_MOTION_REFRESH_MILLIS)
                } while (true)
            }
        }
    }
    return frame
}

private const val DEFAULT_RENDER_DELAY_MILLIS = 1_500L
private const val NANOS_PER_MILLISECOND = 1_000_000L
private const val FREEZE_SETTLE_MILLIS = 400L
private const val REDUCED_MOTION_REFRESH_MILLIS = 1_000L