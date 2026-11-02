@file:Suppress("MagicNumber")

package com.tangem.features.polymarket.common.ui.counter

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_left_16
import com.tangem.core.ui.res.generated.icons.ic_chevron_right_16
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Polymarket counter with a stationary center selection and a scrolling row of values.
 *
 * [Figma](https://www.figma.com/design/dNvLAk1K7ZQ35oWN2aoAAY/Polymarket?node-id=11204-151391)
 *
 * Swipe, item taps and arrows all settle a value in the center before notifying the caller
 * and providing a light haptic tick. External updates and reselecting the same value are silent.
 * The caller owns the committed selection, item order and labels. External selection changes
 * reposition the row without notifying the caller. Missing selections are non-interactive until
 * the caller supplies a valid ID; an empty list never invents a value.
 *
 * @param items Ordered values with unique, stable IDs. Labels need not be unique or numeric.
 * @param selectedId Committed selection. Update it when [onSelectionChange] reports a new ID.
 * @param onSelectionChange Called once for a newly settled user selection, never during scrolling.
 * @param previousContentDescription Localized accessibility label for the previous-value arrow.
 * @param nextContentDescription Localized accessibility label for the next-value arrow.
 * @param modifier Layout modifier. The counter uses the available width, including 16dp side padding.
 */
@Composable
fun PolymarketCounter(
    items: ImmutableList<PolymarketCounter.Item>,
    selectedId: String?,
    onSelectionChange: (String) -> Unit,
    previousContentDescription: String,
    nextContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val ids = remember(items) { items.map { it.id } }
    // Pager's count and key provider must belong to the same list, including during recomposition.
    key(ids) {
        CounterContent(
            items = items,
            selectedId = selectedId,
            onSelectionChange = onSelectionChange,
            previousContentDescription = previousContentDescription,
            nextContentDescription = nextContentDescription,
            modifier = modifier,
        )
    }
}

@Composable
private fun CounterContent(
    items: ImmutableList<PolymarketCounter.Item>,
    selectedId: String?,
    onSelectionChange: (String) -> Unit,
    previousContentDescription: String,
    nextContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val input = remember(items, selectedId) { CounterInput(items.map { it.id }, selectedId) }
    val pager = rememberPagerState(initialPage = input.selectedIndex.coerceAtLeast(0)) { items.size }
    val selection = remember { CounterSelection() }
    val currentInput by rememberUpdatedState(input)
    val onSelection by rememberUpdatedState(onSelectionChange)
    val haptic by rememberUpdatedState(LocalHapticManager.current)
    val scope = rememberCoroutineScope()
    var scrollJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(input) {
        if (selection.needsPositioning(input)) {
            scrollJob?.cancel()
            pager.scrollToPage(input.selectedIndex.coerceAtLeast(0))
        }
        selection.synchronized(input)
    }
    LaunchedEffect(pager) {
        snapshotFlow {
            val isSettled = isCounterSettled(
                isScrolling = pager.isScrollInProgress,
                pageOffsetFraction = pager.currentPageOffsetFraction,
                pageSize = pager.layoutInfo.pageSize,
            )
            if (selection.isSynchronized(currentInput) && isSettled) currentInput to pager.settledPage else null
        }.filterNotNull().collect { (settledInput, index) ->
            selection.selection(settledInput, index)?.let { id ->
                haptic.perform(TangemHapticEffect.View.SegmentTick)
                onSelection(id)
            }
        }
    }

    val onSelect: (Int) -> Unit = { index ->
        if (index in items.indices && input.selectedIndex >= 0) {
            scrollJob?.cancel()
            scrollJob = scope.launch { pager.animateScrollToPage(index) }
        }
    }
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TangemButton(
            variant = TangemButton.Variant.Ghost,
            size = TangemButton.Size.X7,
            iconStart = TangemIconUM.Icon(Icons.ic_chevron_left_16),
            contentDescription = previousContentDescription,
            isEnabled = input.selectedIndex >= 0 && pager.targetPage > 0,
            onClick = { onSelect(pager.targetPage - 1) },
        )
        CounterValues(
            items = items,
            selectedId = selectedId,
            pager = pager,
            onSelect = onSelect,
            modifier = Modifier.weight(1f),
        )
        TangemButton(
            variant = TangemButton.Variant.Ghost,
            size = TangemButton.Size.X7,
            iconStart = TangemIconUM.Icon(Icons.ic_chevron_right_16),
            contentDescription = nextContentDescription,
            isEnabled = input.selectedIndex >= 0 && pager.targetPage < items.lastIndex,
            onClick = { onSelect(pager.targetPage + 1) },
        )
    }
}

/** Public contract of [PolymarketCounter]; selection has no numeric or market-specific meaning. */
object PolymarketCounter {
    /** A stable identity and the caller's ready-to-display label. */
    @Immutable
    data class Item(val id: String, val label: String)
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketCounterPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier.background(TangemTheme.colors3.bg.primary).padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            for (count in listOf(9, 4, 3, 2, 1, 0)) {
                val items = remember(count) {
                    List(count) { PolymarketCounter.Item(id = "$it", label = "$it.5") }.toImmutableList()
                }
                PolymarketCounter(
                    items = items,
                    selectedId = items.getOrNull(count / 2)?.id,
                    onSelectionChange = {},
                    previousContentDescription = "Previous value",
                    nextContentDescription = "Next value",
                )
            }
        }
    }
}