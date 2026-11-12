@file:Suppress("MagicNumber")

package com.tangem.features.polymarket.impl.common.ui.cryptochart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme

@Composable
@Suppress("LongMethod")
internal fun CryptoChartScrubber(
    frame: State<CryptoChartFrame>,
    formatPrice: (Double, Int) -> String,
    formatTime: (Long) -> String,
    modifier: Modifier = Modifier,
) {
    var fraction by remember { mutableStateOf<Float?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { fraction = null }
    val haptic = LocalHapticManager.current
    val color = TangemTheme.colors3.border.accent.yellow
    val grid = TangemTheme.colors3.border.primary
    val configuration = LocalViewConfiguration.current
    val scrubConfiguration = remember(configuration) {
        object : ViewConfiguration by configuration {
            override val longPressTimeoutMillis: Long = SCRUB_PRESS_MILLIS
        }
    }
    val tooltip by remember(frame, formatPrice, formatTime) {
        derivedStateOf {
            val current = frame.value
            val point = fraction?.let(current::pointAt)
            if (point == null) {
                null
            } else {
                val price = formatPrice(point.price, maxOf(MIN_READOUT_DECIMALS, current.scale.fractionDigits(11)))
                price + "\n" + formatTime(point.timestampMillis)
            }
        }
    }
    CompositionLocalProvider(LocalViewConfiguration provides scrubConfiguration) {
        Box(
            modifier.pointerInput(haptic) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { position ->
                        fraction = (position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        haptic.perform(TangemHapticEffect.View.TextHandleMove)
                    },
                    onDragEnd = { fraction = null },
                    onDragCancel = { fraction = null },
                    onDrag = { change, _ ->
                        change.consume()
                        fraction = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    },
                )
            },
        ) {
            Canvas(Modifier.matchParentSize()) {
                val selected = fraction ?: return@Canvas
                val current = frame.value
                val point = current.pointAt(selected) ?: return@Canvas
                val x = current.range.x(point.timestampMillis) * size.width
                drawLine(color = grid, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1.dp.toPx())
                drawCircle(color, 4.dp.toPx(), Offset(x, current.scale.y(point.price) * size.height))
            }
            tooltip?.let { text ->
                TangemSurface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                    isMaterial = true,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = text,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        color = TangemTheme.colors3.text.primary,
                        style = TangemTheme.typography3.caption.medium,
                    )
                }
            }
        }
    }
}

private const val SCRUB_PRESS_MILLIS = 150L

private const val MIN_READOUT_DECIMALS = 2