@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.counter

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
import com.tangem.core.ui.ds2.counter.TangemCounter
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.feature.tester.presentation.storybook.entity.TangemCounterStory

@Composable
internal fun TangemCounterStory(state: TangemCounterStory, modifier: Modifier = Modifier) {
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
                    Section("Значения") {
                        TangemCounterStory.Sample.entries.forEach { sample ->
                            Choice(sample.label, sample == state.sample) { state.onSampleChange(sample) }
                        }
                    }
                    Section("Тема") {
                        Choice("Светлая", !state.isDark) { state.onDarkChange(false) }
                        Choice("Тёмная", state.isDark) { state.onDarkChange(true) }
                    }
                    Section("Размер текста") {
                        listOf(1f, 1.5f, 2f).forEach { scale ->
                            Choice("${(scale * 100).toInt()}%", state.fontScale == scale) {
                                state.onFontScaleChange(scale)
                            }
                        }
                    }
                    Section("Ширина") {
                        Choice("Вся ширина", !state.isNarrow) { state.onNarrowChange(false) }
                        Choice("280 dp", state.isNarrow) { state.onNarrowChange(true) }
                    }
                    Section("Обновление извне") {
                        Choice("Первое / последнее", false, state.onExternalSelection)
                        Choice("Развернуть список", false, state.onReverse)
                        Choice("Удалить выбранное", false, state.onRemoveSelected)
                    }
                }
            }
        }
    }
}

@Composable
private fun CounterPreview(state: TangemCounterStory) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Селектор спредов",
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
                    TangemCounter(
                        items = state.items,
                        selectedId = state.selectedId,
                        onSelectionChange = state.onSelectionChange,
                        previousContentDescription = "Предыдущее значение",
                        nextContentDescription = "Следующее значение",
                        modifier = if (state.isNarrow) Modifier.width(280.dp) else Modifier.fillMaxWidth(),
                    )
                }
            }
            val selected = state.items.firstOrNull { it.id == state.selectedId }
            Text(
                text = "Выбрано: ${selected?.label ?: "—"}  ·  изменений: ${state.selectionCount}",
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
        Text(
            text = "Проведите по значениям, нажмите на число или стрелку. " +
                "Выбор меняется после остановки в центре.",
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