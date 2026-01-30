package com.tangem.common.ui.news

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.label.Label
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.ImmutableList

private val TAG_SPACING = 4.dp

/**
 * A layout that displays a list of tags, automatically calculating how many can fit
 * in a single line. If not all tags fit, it shows an overflow indicator (e.g., "+N").
 *
 * @param tags The immutable list of [LabelUM] to display.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
internal fun Tags(tags: ImmutableList<LabelUM>, modifier: Modifier = Modifier) {
    if (tags.isEmpty()) return

    SubcomposeLayout(modifier = modifier) { constraints ->
        val spacingPx = TAG_SPACING.roundToPx()

        val tagPlaceables = subcompose(ContentSlot.Tags) {
            tags.forEach { tag ->
                Label(state = tag)
            }
        }.map { it.measure(constraints) }

        val layoutInfo = calculateLayoutInfo(
            maxWidth = constraints.maxWidth,
            spacingPx = spacingPx,
            tagPlaceables = tagPlaceables,
        )

        val overflowPlaceable = layoutInfo.overflowCount.takeIf { it > 0 }?.let {
            subcompose(ContentSlot.Overflow) {
                OverflowLabel(count = it)
            }.first().measure(constraints)
        }

        val visiblePlaceables = tagPlaceables.take(layoutInfo.visibleCount)
        val allVisibleItems = visiblePlaceables + listOfNotNull(overflowPlaceable)

        val width = calculateRowWidth(
            placeables = allVisibleItems,
            spacingPx = spacingPx,
        ).coerceIn(constraints.minWidth, constraints.maxWidth)

        val height = allVisibleItems.maxOfOrNull { it.height } ?: 0

        layout(width = width, height = height) {
            var xPosition = 0
            allVisibleItems.forEachIndexed { index, placeable ->
                if (index > 0) {
                    xPosition += spacingPx
                }
                val yPosition = height - placeable.height
                placeable.placeRelative(xPosition, yPosition)
                xPosition += placeable.width
            }
        }
    }
}

/**
 * Calculates how many tags can be displayed and how many are overflowing.
 */
private fun SubcomposeMeasureScope.calculateLayoutInfo(
    maxWidth: Int,
    spacingPx: Int,
    tagPlaceables: List<Placeable>,
): TagsLayoutInfo {
    val totalWidthWithoutOverflow = calculateRowWidth(tagPlaceables, spacingPx)

    if (totalWidthWithoutOverflow <= maxWidth) {
        return TagsLayoutInfo(visibleCount = tagPlaceables.size, overflowCount = 0)
    }

    for (count in tagPlaceables.indices.reversed()) {
        val visibleTags = tagPlaceables.take(count)
        val overflowCount = tagPlaceables.size - count

        val tempOverflowPlaceable = subcompose("overflow_temp_$overflowCount") {
            OverflowLabel(count = overflowCount)
        }.first().measure(Constraints())

        val widthWithOverflow = calculateRowWidth(
            placeables = visibleTags + tempOverflowPlaceable,
            spacingPx = spacingPx,
        )

        if (widthWithOverflow <= maxWidth) {
            return TagsLayoutInfo(visibleCount = count, overflowCount = overflowCount)
        }
    }

    return TagsLayoutInfo(visibleCount = 0, overflowCount = tagPlaceables.size)
}

@Composable
private fun OverflowLabel(count: Int) {
    Label(
        state = LabelUM(
            text = TextReference.Str("${StringsSigns.PLUS}$count"),
            maxLines = 1,
        ),
    )
}

private fun calculateRowWidth(placeables: List<Placeable>, spacingPx: Int): Int {
    if (placeables.isEmpty()) return 0
    val tagsWidth = placeables.sumOf { it.width }
    val spacingWidth = spacingPx * (placeables.size - 1).coerceAtLeast(0)
    return tagsWidth + spacingWidth
}

private data class TagsLayoutInfo(val visibleCount: Int, val overflowCount: Int)

private enum class ContentSlot { Tags, Overflow }