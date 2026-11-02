@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.polymarket.counter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.feature.tester.presentation.storybook.entity.PolymarketCounterStory
import com.tangem.features.polymarket.common.ui.counter.PolymarketCounter

@Composable
internal fun PolymarketCounterStory(state: PolymarketCounterStory, modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalIsInDarkTheme provides state.isDark) {
        TangemThemeRedesign {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(TangemTheme.colors3.bg.primary)
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                CounterPreview(state)
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Section("Values") {
                        PolymarketCounterStory.Sample.entries.forEach { sample ->
                            Choice(sample.label, sample == state.sample) { state.onSampleChange(sample) }
                        }
                    }
                    Section("Theme") {
                        Choice("Light", !state.isDark) { state.onDarkChange(false) }
                        Choice("Dark", state.isDark) { state.onDarkChange(true) }
                    }
                    Section("Text size") {
                        listOf(1f, 1.5f, 2f).forEach { scale ->
                            Choice("${(scale * 100).toInt()}%", state.fontScale == scale) {
                                state.onFontScaleChange(scale)
                            }
                        }
                    }
                    Section("Width") {
                        Choice("Full width", !state.isNarrow) { state.onNarrowChange(false) }
                        Choice("280 dp", state.isNarrow) { state.onNarrowChange(true) }
                    }
                    Section("External updates") {
                        Choice("First / last", false, state.onExternalSelection)
                        Choice("Reverse order", false, state.onReverse)
                        Choice("Remove selected", false, state.onRemoveSelected)
                    }
                }
            }
        }
    }
}

@Composable
private fun CounterPreview(state: PolymarketCounterStory) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Spread selector",
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors3.bg.secondary, RoundedCornerShape(16.dp))
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val density = LocalDensity.current
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, density.fontScale * state.fontScale),
                ) {
                    PolymarketCounter(
                        items = state.items,
                        selectedId = state.selectedId,
                        onSelectionChange = state.onSelectionChange,
                        previousContentDescription = "Previous value",
                        nextContentDescription = "Next value",
                        modifier = if (state.isNarrow) Modifier.width(280.dp) else Modifier.fillMaxWidth(),
                    )
                }
            }
            val selected = state.items.firstOrNull { it.id == state.selectedId }
            Text(
                text = "Selected: ${selected?.label ?: "—"}  ·  changes: ${state.selectionCount}",
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
        Text(
            text = "Swipe the values, tap a number or use the arrows. " +
                "Feel a tick as each value crosses the center. Selection changes after snapping.",
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.primary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun Choice(label: String, isSelected: Boolean, onClick: () -> Unit) {
    TangemButton(
        text = stringReference(label),
        variant = if (isSelected) TangemButton.Variant.Primary else TangemButton.Variant.Secondary,
        size = TangemButton.Size.X8,
        onClick = onClick,
    )
}