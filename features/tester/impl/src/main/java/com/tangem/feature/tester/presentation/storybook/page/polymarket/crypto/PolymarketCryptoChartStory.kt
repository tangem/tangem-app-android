@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.polymarket.crypto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import com.tangem.utils.time.CurrentTimeProvider
import com.tangem.features.polymarket.impl.common.ui.cryptochart.PolymarketLiveCryptoChart
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.feature.tester.presentation.storybook.entity.PolymarketCryptoChartStory
import com.tangem.feature.tester.presentation.storybook.page.polymarket.chart.ChartChoice
import com.tangem.feature.tester.presentation.storybook.page.polymarket.chart.ChartChoices
import com.tangem.features.polymarket.impl.common.ui.cryptochart.PolymarketCryptoChart
import com.tangem.features.polymarket.impl.common.ui.cryptochart.PolymarketCryptoChartMini
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
@Suppress("LongMethod")
internal fun PolymarketCryptoChartStory(state: PolymarketCryptoChartStory, modifier: Modifier = Modifier) {
    val priceFormatter = remember { CryptoDemoPriceFormatter() }
    val snapshot = remember(state.sample, state.interval, state.target) {
        cryptoFixture(state.sample, state.interval, state.target)
    }
    var generation by remember { mutableIntStateOf(0) }
    val clock = remember(state.sample, state.interval, generation, state.isLive, state.updateMode) {
        val origin = System.currentTimeMillis()
        CurrentTimeProvider { snapshot.range.endMillis + System.currentTimeMillis() - origin }
    }
    var freezeAt by remember(clock) { mutableStateOf<Long?>(null) }
    val revision by rememberCryptoRevision(
        state = state,
        clock = clock,
        startMillis = snapshot.range.endMillis,
        freezeAt = freezeAt,
    )
    val points = remember(
        snapshot,
        revision,
        state.updateMode,
    ) {
        if (state.sample == PolymarketCryptoChartStory.Sample.Narrowing) {
            snapshot.points
        } else {
            liveCryptoPoints(snapshot, revision, state.updateMode)
        }
    }
    val rangePolicy = remember(state.interval) {
        { now: Long -> PolymarketCryptoChart.TimeRange(now - state.interval.millis, now) }
    }
    val time = remember(state.interval) {
        SimpleDateFormat(
            if (state.interval == PolymarketCryptoChartStory.Interval.Minute) "HH:mm:ss" else "HH:mm",
            Locale.US,
        )
    }
    CompositionLocalProvider(LocalIsInDarkTheme provides state.isDark) {
        TangemThemeRedesign {
            Column(modifier.fillMaxSize().background(TangemTheme.colors3.bg.primary).padding(16.dp)) {
                Text(
                    "Crypto price chart",
                    color = TangemTheme.colors3.text.primary,
                    style = TangemTheme.typography3.heading.small,
                )
                Spacer(Modifier.height(16.dp))
                val density = LocalDensity.current
                Box(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    CompositionLocalProvider(
                        LocalDensity provides Density(density.density, density.fontScale * state.fontScale),
                    ) {
                        val digits = if (state.sample == PolymarketCryptoChartStory.Sample.Tiny) 9 else 2
                        val target =
                            stringReference("Target ${priceFormatter.format(snapshot.targetPrice ?: 0.0, digits)}")
                        if (state.isCompact) {
                            TangemSurface(
                                color = TangemTheme.colors3.bg.secondary,
                                shape = RoundedCornerShape(24.dp),
                            ) {
                                PolymarketCryptoChartMini(
                                    snapshot = snapshot,
                                    targetLabel = target,
                                    emptyLabel = stringReference("No data"),
                                    formatPrice = priceFormatter::format,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        } else if (state.isLive) {
                            PolymarketLiveCryptoChart(
                                points = points,
                                targetPrice = snapshot.targetPrice,
                                windowId = "${state.sample}-${state.interval}-${state.updateMode}-$generation",
                                timeRange = rangePolicy,
                                timeProvider = clock,
                                targetLabel = target,
                                emptyLabel = stringReference("No data"),
                                formatPrice = priceFormatter::format,
                                formatTime = { time.format(Date(it)) },
                                freezeAtMillis = freezeAt,
                            )
                        } else {
                            PolymarketCryptoChart(
                                snapshot = snapshot,
                                targetLabel = target,
                                emptyLabel = stringReference("No data"),
                                formatPrice = priceFormatter::format,
                                formatTime = { time.format(Date(it)) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (!state.isCompact) {
                        ChartChoices("Playback") {
                            ChartChoice(if (state.isLive) "Stop live" else "Start live", state.isLive) {
                                state.onLive(!state.isLive)
                            }
                            ChartChoice("Finish interval", freezeAt != null) { freezeAt = clock.nowMillis() - 1_500 }
                            ChartChoice("New window", false) { generation++ }
                        }
                        ChartChoices("Quotes") {
                            PolymarketCryptoChartStory.UpdateMode.entries.forEach { mode ->
                                ChartChoice(mode.name, mode == state.updateMode) { state.onUpdateMode(mode) }
                            }
                        }
                    }
                    CryptoChartControls(state)
                }
            }
        }
    }
}

@Composable
private fun rememberCryptoRevision(
    state: PolymarketCryptoChartStory,
    clock: CurrentTimeProvider,
    startMillis: Long,
    freezeAt: Long?,
): State<Int> {
    val revision = remember(clock) { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state.isLive, clock, freezeAt, lifecycle) {
        if (state.isLive && freezeAt == null && state.sample != PolymarketCryptoChartStory.Sample.Narrowing) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    val isFast = state.updateMode == PolymarketCryptoChartStory.UpdateMode.Fast
                    if (isFast) withFrameNanos { }
                    revision.intValue = ((clock.nowMillis() - startMillis) / state.updateMode.cadenceMillis)
                        .toInt().coerceAtLeast(0)
                    if (!isFast) delay(state.updateMode.cadenceMillis)
                }
            }
        }
    }
    return revision
}

@Composable
@Suppress("LongMethod")
private fun CryptoChartControls(state: PolymarketCryptoChartStory, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ChartChoices("Variant") {
            ChartChoice("Full", !state.isCompact) { state.onCompact(false) }
            ChartChoice("Card", state.isCompact) { state.onCompact(true) }
        }
        ChartChoices("Data") {
            PolymarketCryptoChartStory.Sample.entries.forEach { item ->
                ChartChoice(item.name, state.sample == item) { state.onSample(item) }
            }
        }
        ChartChoices("Visible interval") {
            PolymarketCryptoChartStory.Interval.entries.forEach { item ->
                ChartChoice(item.label, state.interval == item) { state.onInterval(item) }
            }
        }
        ChartChoices("Target") {
            PolymarketCryptoChartStory.Target.entries.forEach { item ->
                ChartChoice(item.name, state.target == item) { state.onTarget(item) }
            }
        }
        ChartChoices("Theme") {
            ChartChoice("Light", !state.isDark) { state.onDark(false) }
            ChartChoice("Dark", state.isDark) { state.onDark(true) }
        }
        ChartChoices("Text size") {
            listOf(1f, 1.5f, 2f).forEach { value ->
                ChartChoice("${(value * 100).toInt()}%", state.fontScale == value) { state.onFontScale(value) }
            }
        }
    }
}