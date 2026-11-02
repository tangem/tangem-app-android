@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.polymarket.chart

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.feature.tester.presentation.storybook.entity.PolymarketProbabilityChartStory
import com.tangem.feature.tester.presentation.storybook.entity.StoryPageFactory
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

internal val probabilityChartStoryFactory = StoryPageFactory { PolymarketProbabilityChartStory }

@Composable
@Suppress("LongMethod")
internal fun PolymarketProbabilityChartStory(modifier: Modifier = Modifier) {
    var sample by rememberSaveable { mutableStateOf(ChartSample.Sports) }
    var period by rememberSaveable { mutableStateOf("6h") }
    var isDark by rememberSaveable { mutableStateOf(false) }
    var fontScale by rememberSaveable { mutableFloatStateOf(1f) }
    var isNarrow by rememberSaveable { mutableStateOf(false) }
    var revision by rememberSaveable { mutableIntStateOf(0) }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(isPlaying, lifecycle) {
        if (isPlaying) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(DEMO_UPDATE_INTERVAL_MILLIS)
                    revision++
                }
            }
        }
    }
    val content = remember(sample, period, revision) { chartFixture(sample, period, revision) }
    val formatter = remember(period) { SimpleDateFormat(if (period == "Max") "MMM d" else "HH:mm", Locale.ENGLISH) }
    CompositionLocalProvider(LocalIsInDarkTheme provides isDark) {
        TangemThemeRedesign {
            Column(
                modifier.fillMaxSize().background(
                    TangemTheme.colors3.bg.secondary,
                ).verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Probability chart",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = TangemTheme.colors3.text.primary,
                    style = TangemTheme.typography3.heading.small,
                )
                val density = LocalDensity.current
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CompositionLocalProvider(
                        LocalDensity provides Density(density.density, density.fontScale * fontScale),
                    ) {
                        PolymarketProbabilityChart(
                            state = content,
                            periods = chartPeriods,
                            selectedPeriodId = period,
                            onPeriodChange = { id -> period = id; revision = 0; isPlaying = false },
                            formatTime = { formatter.format(Date(it)) },
                            emptyTitle = stringReference("No data yet"),
                            emptyDescription = stringReference("The chart will appear after the first predictions"),
                            legendLineCount = if (sample.count == 2) 1 else 2,
                            modifier = if (isNarrow) Modifier.width(280.dp) else Modifier.fillMaxWidth(),
                        )
                    }
                }
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ChartChoices("Updates") {
                        ChartChoice("Append live point", false) { revision++ }
                        ChartChoice(if (isPlaying) "Pause live updates" else "Start live updates", isPlaying) {
                            if (!isPlaying) {
                                period = "Live"
                                if (sample !in listOf(ChartSample.Binary, ChartSample.Sports, ChartSample.Multiple)) {
                                    sample = ChartSample.Sports
                                }
                            }
                            isPlaying = !isPlaying
                        }
                    }
                    Text(
                        text = "Touch and drag to inspect every outcome at the same time. " +
                            "Release to return to the latest values.",
                        color = TangemTheme.colors3.text.secondary,
                        style = TangemTheme.typography3.subheading.medium,
                    )
                    ChartChoices("Data") {
                        ChartSample.entries.forEach { item ->
                            ChartChoice(item.label, item == sample) { sample = item; revision = 0; isPlaying = false }
                        }
                    }
                    ChartChoices("Theme") {
                        ChartChoice("Light", !isDark) { isDark = false }
                        ChartChoice("Dark", isDark) { isDark = true }
                    }
                    ChartChoices("Text size") {
                        listOf(
                            1f,
                            1.5f,
                            2f,
                        ).forEach { size -> ChartChoice(
                            "${(size * 100).toInt()}%",
                            size == fontScale,
                        ) { fontScale = size } }
                    }
                    ChartChoices("Width") {
                        ChartChoice("Full width", !isNarrow) { isNarrow = false }
                        ChartChoice("280 dp", isNarrow) { isNarrow = true }
                    }
                }
            }
        }
    }
}

private const val DEMO_UPDATE_INTERVAL_MILLIS = 2_000L

@Composable
private fun ChartChoices(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = TangemTheme.colors3.text.primary, style = TangemTheme.typography3.subheading.medium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { content() }
    }
}

@Composable
private fun ChartChoice(label: String, isSelected: Boolean, onClick: () -> Unit) {
    TangemButton(
        text = stringReference(label),
        onClick = onClick,
        variant = if (isSelected) TangemButton.Variant.Primary else TangemButton.Variant.Secondary,
        size = TangemButton.Size.X8,
    )
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProbabilityChartPreview() {
    TangemThemePreviewRedesign {
        PolymarketProbabilityChart(
            state = chartFixture(ChartSample.Sports, "6h", 0),
            periods = chartPeriods,
            selectedPeriodId = "6h",
            onPeriodChange = {},
            formatTime = { "12:00" },
            emptyTitle = stringReference("No data yet"),
            emptyDescription = stringReference("The chart will appear after the first predictions"),
            modifier = Modifier.fillMaxWidth().background(TangemTheme.colors3.bg.secondary),
        )
    }
}