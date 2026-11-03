@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.polymarket.crypto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.stringReference
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
internal fun PolymarketCryptoChartStory(state: PolymarketCryptoChartStory, modifier: Modifier = Modifier) {
    val snapshot = remember(state.sample, state.interval, state.target) {
        cryptoFixture(state.sample, state.interval, state.target)
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
                            stringReference("Target ${formatCryptoDemoPrice(snapshot.targetPrice ?: 0.0, digits)}")
                        if (state.isCompact) {
                            PolymarketCryptoChartMini(
                                snapshot = snapshot,
                                targetLabel = target,
                                emptyLabel = stringReference("No data"),
                                formatPrice = ::formatCryptoDemoPrice,
                            )
                        } else {
                            PolymarketCryptoChart(
                                snapshot = snapshot,
                                targetLabel = target,
                                emptyLabel = stringReference("No data"),
                                formatPrice = ::formatCryptoDemoPrice,
                                formatTime = { time.format(Date(it)) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                CryptoChartControls(state, Modifier.weight(1f).verticalScroll(rememberScrollState()))
            }
        }
    }
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