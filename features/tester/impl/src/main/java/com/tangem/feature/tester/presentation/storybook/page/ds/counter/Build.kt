@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.counter

import com.tangem.core.ui.ds2.counter.TangemCounter
import com.tangem.feature.tester.presentation.storybook.entity.TangemCounterStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemCounterStory.Sample
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory
import kotlinx.collections.immutable.toImmutableList

internal val tangemCounterStoryFactory
    get() = storyPageFactory(StateUpdater<TangemCounterStory>::buildCounter)

private fun StateUpdater<TangemCounterStory>.buildCounter(): TangemCounterStory {
    return TangemCounterStory(
        sample = Sample.Many,
        items = Sample.Many.items(),
        selectedId = "value-4",
        selectionCount = 0,
        isDark = false,
        isNarrow = false,
        fontScale = 1f,
        onSampleChange = { sample ->
            val items = sample.items()
            updateStory { state ->
                state.copy(
                    sample = sample,
                    items = items,
                    selectedId = items.getOrNull(items.size / 2)?.id,
                    selectionCount = 0,
                )
            }
        },
        onSelectionChange = { id ->
            updateStory { state -> state.copy(selectedId = id, selectionCount = state.selectionCount + 1) }
        },
        onDarkChange = { isDark -> updateStory { it.copy(isDark = isDark) } },
        onNarrowChange = { isNarrow -> updateStory { it.copy(isNarrow = isNarrow) } },
        onFontScaleChange = { fontScale -> updateStory { it.copy(fontScale = fontScale) } },
        onExternalSelection = {
            updateStory { state ->
                val target = if (state.selectedId == state.items.firstOrNull()?.id) {
                    state.items.lastOrNull()?.id
                } else {
                    state.items.firstOrNull()?.id
                }
                state.copy(selectedId = target)
            }
        },
        onReverse = { updateStory { it.copy(items = it.items.reversed().toImmutableList()) } },
        onRemoveSelected = {
            updateStory { state ->
                val remaining = state.items.filterNot { it.id == state.selectedId }.toImmutableList()
                state.copy(items = remaining, selectedId = remaining.firstOrNull()?.id)
            }
        },
    )
}

private fun Sample.items() = when (this) {
    Sample.Many -> List(9) { "$it.5" }
    Sample.Four -> List(4) { "$it.5" }
    Sample.Three -> List(3) { "$it.5" }
    Sample.Two -> List(2) { "$it.5" }
    Sample.One -> listOf("1.5")
    Sample.Empty -> emptyList()
    Sample.Negative -> listOf("−4.5", "−3.5", "−2.5", "−1.5", "−0.5", "+0.5", "+1.5", "+2.5", "+3.5")
    Sample.Long -> listOf("−125.5", "−100.5", "−75.5", "−25.5", "+25.5", "+75.5", "+100.5", "+125.5")
    Sample.Duplicate -> List(7) { "1.5" }
}.mapIndexed { index, label -> TangemCounter.Item(id = "value-$index", label = label) }.toImmutableList()