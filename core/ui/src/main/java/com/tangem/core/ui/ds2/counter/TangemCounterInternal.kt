@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.counter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun CounterValues(
    items: ImmutableList<TangemCounter.Item>,
    selectedId: String?,
    pager: PagerState,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = TangemTheme.typography3.subheading.medium
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val sizes = remember(items, style, density) {
        items.map { measurer.measure(it.label, style = style, maxLines = 1).size }
    }
    val textWidth = with(density) { (sizes.maxOfOrNull { it.width } ?: 0).toDp() }
    val textHeight = with(density) { (sizes.maxOfOrNull { it.height } ?: 0).toDp() }
    val rowHeight = (textHeight + 4.dp).coerceAtLeast(28.dp)
    val hasSelection = items.any { it.id == selectedId }
    BoxWithConstraints(modifier = modifier.height(rowHeight), contentAlignment = Alignment.Center) {
        val visibleCount = when {
            maxWidth / 5 >= textWidth + 24.dp -> 5
            maxWidth / 3 >= textWidth + 24.dp -> 3
            else -> 1
        }
        val slotWidth = maxWidth / visibleCount
        LaunchedEffect(slotWidth, rowHeight) {
            val selectedIndex = items.indexOfFirst { it.id == selectedId }
            if (selectedIndex >= 0) pager.scrollToPage(selectedIndex)
        }
        if (hasSelection) {
            Box(
                modifier = Modifier
                    .width((textWidth + 16.dp).coerceAtMost(slotWidth))
                    .height(textHeight + 4.dp)
                    .background(TangemTheme.colors3.bg.transparent.primary, RoundedCornerShape(6.dp)),
            )
        }
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            pageSize = PageSize.Fixed(slotWidth),
            contentPadding = PaddingValues(horizontal = ((maxWidth - slotWidth) / 2).coerceAtLeast(0.dp)),
            snapPosition = SnapPosition.Center,
            beyondViewportPageCount = 2,
            key = { items[it].id },
            userScrollEnabled = hasSelection && items.size > 1,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pager,
                pagerSnapDistance = PagerSnapDistance.atMost(items.size.coerceAtLeast(1)),
            ),
        ) { index ->
            CounterValue(
                item = items[index],
                isSelected = items[index].id == selectedId,
                isCentered = hasSelection && index == pager.currentPage,
                isEnabled = hasSelection,
                onClick = { onSelect(index) },
            )
        }
    }
}

@Composable
private fun CounterValue(
    item: TangemCounter.Item,
    isSelected: Boolean,
    isCentered: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(6.dp))
            .selectable(
                selected = isSelected,
                enabled = isEnabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = item.label,
            style = TangemTheme.typography3.subheading.medium,
            color = if (isCentered) TangemTheme.colors3.text.primary else TangemTheme.colors3.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}