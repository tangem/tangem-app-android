@file:Suppress("MagicNumber", "LongMethod")

package com.tangem.feature.tester.presentation.storybook.page.polymarket.chart

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.feature.tester.presentation.storybook.entity.PolymarketEventChartStory
import com.tangem.feature.tester.presentation.storybook.entity.StoryPageFactory
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart
import com.tangem.features.polymarket.impl.details.model.chart.PolymarketChartContent
import com.tangem.features.polymarket.impl.details.model.chart.PolymarketChartPeriod
import com.tangem.features.polymarket.impl.details.model.chart.PolymarketEventChartSession
import com.tangem.features.polymarket.impl.details.ui.preview.PolymarketEventDetailsChartPreview
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val eventChartStoryFactory = StoryPageFactory { PolymarketEventChartStory }

@Composable
internal fun PolymarketEventChartStory(modifier: Modifier = Modifier) {
    var sample by rememberSaveable { mutableStateOf(EventChartSample.Sports) }
    var response by rememberSaveable { mutableStateOf(EventChartResponse.Success) }
    var revision by rememberSaveable { mutableIntStateOf(0) }
    var isShowingDetails by rememberSaveable { mutableStateOf(true) }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var isDark by rememberSaveable { mutableStateOf(false) }
    var fontScale by rememberSaveable { mutableFloatStateOf(1f) }
    val fixtureTime = remember { System.currentTimeMillis() }
    val event = remember(sample, revision) { eventChartFixture(sample, revision, fixtureTime) }
    val conditions =
        remember(sample) { if (sample == EventChartSample.Sports) setOf("Team A", "Team B", "Draw") else null }
    val session =
        remember { PolymarketEventChartSession(PolymarketChartContent.create(event, fixtureTime, conditions)) }
    val state by session.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val formatter = remember(state.selectedPeriod) {
        SimpleDateFormat(if (state.selectedPeriod == PolymarketChartPeriod.Max) "MMM d" else "HH:mm", Locale.ENGLISH)
    }
    EventChartRequests(session = session, event = event, conditions = conditions, response = response)
    LaunchedEffect(isPlaying, isShowingDetails, lifecycle) {
        if (isPlaying && isShowingDetails) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) { delay(2_000); revision++ }
            }
        }
    }
    BackHandler(enabled = isShowingDetails) { isShowingDetails = false }
    CompositionLocalProvider(LocalIsInDarkTheme provides isDark) {
        TangemThemeRedesign {
            if (isShowingDetails) {
                val density = LocalDensity.current
                Box(modifier.fillMaxSize().background(TangemTheme.colors3.bg.primary)) {
                    CompositionLocalProvider(
                        LocalDensity provides Density(density.density, density.fontScale * fontScale),
                    ) {
                        PolymarketEventDetailsChartPreview(event, onBackClick = { isShowingDetails = false }) {
                            PolymarketProbabilityChart(
                                state = state.chart,
                                periods = state.content.periods.map { period ->
                                    val title = period.demoTitle(state.content.isLive)
                                    PolymarketProbabilityChart.Period(period.id, stringReference(title))
                                }.toImmutableList(),
                                selectedPeriodId = state.selectedPeriod.id,
                                onPeriodChange = { id -> session.selectPeriod(
                                    PolymarketChartPeriod.entries.first { it.id == id },
                                    System.currentTimeMillis(),
                                ) },
                                formatTime = { formatter.format(Date(it)) },
                                emptyTitle = stringReference("No data yet"),
                                emptyDescription = stringReference("The chart will appear after the first predictions"),
                                legendLineCount = if (state.content.outcomes.size <= 2) 1 else 2,
                            )
                        }
                    }
                    Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp)) {
                        ChartChoice("Scenarios", false) { isShowingDetails = false }
                    }
                }
            } else {
                Column(
                    modifier.fillMaxSize().background(TangemTheme.colors3.bg.primary).systemBarsPadding()
                        .verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        "Event chart scenarios",
                        color = TangemTheme.colors3.text.primary,
                        style = TangemTheme.typography3.heading.small,
                    )
                    ChartChoice("Open event details", true) { isShowingDetails = true }
                    ChartChoices("Event") {
                        EventChartSample.entries.forEach { value -> ChartChoice(
                            value.name,
                            sample == value,
                        ) { sample = value; revision = 0 } }
                    }
                    ChartChoices("Next history response") {
                        EventChartResponse.entries.forEach { value -> ChartChoice(
                            value.name,
                            response == value,
                        ) { response = value } }
                    }
                    ChartChoices("Updates") {
                        ChartChoice("Reload history", false) { session.reload(System.currentTimeMillis()) }
                        ChartChoice("Update prices", false) { revision++ }
                        ChartChoice(
                            if (isPlaying) "Pause live updates" else "Start live updates",
                            isPlaying,
                        ) { isPlaying = !isPlaying }
                    }
                    Text(
                        "Selected: ${state.selectedPeriod.id} · Loading: ${state.request?.period?.id ?: "none"}" +
                            " · Failure: ${state.hasFailure}",
                        color = TangemTheme.colors3.text.secondary,
                        style = TangemTheme.typography3.caption.medium,
                    )
                    ChartChoices("Theme") {
                        ChartChoice("Light", !isDark) { isDark = false }
                        ChartChoice("Dark", isDark) { isDark = true }
                    }
                    ChartChoices("Text size") {
                        listOf(
                            1f,
                            1.5f,
                            2f,
                        ).forEach { scale -> ChartChoice(
                            "${(scale * 100).toInt()}%",
                            fontScale == scale,
                        ) { fontScale = scale } }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventChartRequests(
    session: PolymarketEventChartSession,
    event: PolymarketEvent,
    conditions: Set<String>?,
    response: EventChartResponse,
) {
    val state by session.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(event) {
        session.refresh(
            PolymarketChartContent.create(
                event = event,
                nowMillis = System.currentTimeMillis(),
                moneylineConditionIds = conditions,
                previous = state.content,
            ),
            System.currentTimeMillis(),
        )
    }
    LaunchedEffect(state.request) {
        val request = state.request ?: return@LaunchedEffect
        val nextResponse = response
        delay(if (nextResponse == EventChartResponse.Slow) 3_000 else 500)
        session.apply(
            request,
            eventChartHistory(request, nextResponse, System.currentTimeMillis()),
            System.currentTimeMillis(),
        )
    }
    DisposableEffect(lifecycle, session) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> session.reload(System.currentTimeMillis())
                Lifecycle.Event.ON_STOP -> session.cancelLoading(System.currentTimeMillis())
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        session.reload(System.currentTimeMillis())
        onDispose { lifecycle.removeObserver(observer); session.cancelLoading(System.currentTimeMillis()) }
    }
}

private fun PolymarketChartPeriod.demoTitle(isLive: Boolean): String = when {
    this == PolymarketChartPeriod.Hour && isLive -> "Live"
    this == PolymarketChartPeriod.Max -> "Max"
    else -> id
}