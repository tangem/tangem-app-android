package com.tangem.feature.tester.presentation.storybook.entity

import com.tangem.features.polymarket.common.ui.counter.PolymarketCounter
import kotlinx.collections.immutable.ImmutableList

internal data class PolymarketCounterStory(
    val sample: Sample,
    val items: ImmutableList<PolymarketCounter.Item>,
    val selectedId: String?,
    val selectionCount: Int,
    val isDark: Boolean,
    val isNarrow: Boolean,
    val fontScale: Float,
    val onSampleChange: (Sample) -> Unit,
    val onSelectionChange: (String) -> Unit,
    val onDarkChange: (Boolean) -> Unit,
    val onNarrowChange: (Boolean) -> Unit,
    val onFontScaleChange: (Float) -> Unit,
    val onExternalSelection: () -> Unit,
    val onReverse: () -> Unit,
    val onRemoveSelected: () -> Unit,
) : StoryBookPage {
    enum class Sample(val label: String) {
        Many("9"), Four("4"), Three("3"), Two("2"), One("1"), Empty("0"),
        Negative("−1.5"), Long("−125.5"), Duplicate("Duplicates"),
    }
}