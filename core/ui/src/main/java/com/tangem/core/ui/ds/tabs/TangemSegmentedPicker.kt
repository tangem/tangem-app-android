package com.tangem.core.ui.ds.tabs

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.utils.extensions.indexOfFirstOrNull
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Tangem segmented picker component.
 * [Figma](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=8452-16487&m=dev)
 *
 * @param tangemSegmentedPickerUM Model representing the segmented picker.
 * @param modifier                Modifier to be applied to the segmented picker.
 * @param onClick                 Lambda function to be invoked when a segment is clicked.
 */
@Composable
fun TangemSegmentedPicker(
    tangemSegmentedPickerUM: TangemSegmentedPickerUM,
    modifier: Modifier = Modifier,
    onClick: (TangemSegmentUM) -> Unit,
) {
    TangemSegmentedPicker(
        items = tangemSegmentedPickerUM.items,
        modifier = modifier,
        initialSelectedItem = tangemSegmentedPickerUM.initialSelectedItem,
        isFixed = tangemSegmentedPickerUM.isFixed,
        isAltSurface = tangemSegmentedPickerUM.isAltSurface,
        onClick = onClick,
    )
}

/**
 * Tangem segmented picker component.
 * [Figma](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=8452-16487&m=dev)
 *
 * @param items                   List of TangemSegmentUM representing the segments in the picker.
 * @param modifier                Modifier to be applied to the segmented picker.
 * @param initialSelectedItem     Optional TangemSegmentUM representing the initially selected segment.
 * @param isFixed                 Boolean indicating whether the picker has a fixed width.
 * @param isAltSurface            Boolean indicating whether to use an alternative surface style.
 * @param onClick                 Lambda function to be invoked when a segment is clicked.
 */
@Composable
fun TangemSegmentedPicker(
    items: ImmutableList<TangemSegmentUM>,
    modifier: Modifier = Modifier,
    initialSelectedItem: TangemSegmentUM? = null,
    isFixed: Boolean = false,
    isAltSurface: Boolean = false,
    minSegmentWidth: Dp = Dp.Unspecified,
    onClick: (TangemSegmentUM) -> Unit,
) {
    if (items.isEmpty() || items.size == 1) return

    val density = LocalDensity.current

    val itemsWidths = remember { mutableStateListOf(*Array(items.size) { 0.dp }) }
    val selectedIndex = remember { mutableIntStateOf(items.indexOfFirstOrNull { it == initialSelectedItem } ?: 0) }
    val segmentHeight = remember { mutableStateOf(0.dp) }

    val shape = RoundedCornerShape(TangemTheme.dimens2.x25)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                color = if (isAltSurface) {
                    TangemTheme.colors2.tabs.backgroundQuaternary
                } else {
                    TangemTheme.colors2.tabs.backgroundSecondary
                },
            )
            .padding(TangemTheme.dimens2.x0_5),
    ) {
        SegmentSelection(
            itemsWidths = itemsWidths,
            selectedIndex = selectedIndex.intValue,
            segmentHeight = segmentHeight.value,
            separatorWidth = SEPARATOR_WIDTH,
        )
        SegmentsRow(
            isFixed = isFixed,
            minSegmentWidth = minSegmentWidth,
            segmentCount = items.size,
            content = {
                items.fastForEachIndexed { index, item ->
                    Segment(
                        item = item,
                        index = index,
                        minSegmentWidth = minSegmentWidth,
                        selectedIndex = selectedIndex,
                        onClick = { onClick(item) },
                        modifier = Modifier
                            .onGloballyPositioned {
                                with(density) {
                                    itemsWidths[index] = it.size.width.toDp()
                                    segmentHeight.value = it.size.height.toDp().coerceAtLeast(segmentHeight.value)
                                }
                            },
                    )

                    if (index != items.lastIndex) {
                        Separator(index = index, selectedIndex = selectedIndex)
                    }
                }
            },
        )
    }
}

/**
 * Lays out segments (with the inter-segment separators interleaved) in a single row.
 *
 * - When [isFixed] is `true` and the parent width is bounded, every segment is sized to at least its
 *   content width, and the remaining free space is distributed equally between segments — so the row
 *   fills the full width without clipping any segment's text.
 * - Otherwise segments wrap their content.
 *
 * Children must be supplied as `segment, separator, segment, separator, … , segment` (even indices are
 * segments, odd indices are separators).
 */
@Composable
private fun SegmentsRow(
    isFixed: Boolean,
    minSegmentWidth: Dp,
    segmentCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val segMeasurables = measurables.filterIndexed { i, _ -> i % 2 == 0 }
        val sepMeasurables = measurables.filterIndexed { i, _ -> i % 2 == 1 }

        val sepPlaceables = sepMeasurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val totalSeparators = sepPlaceables.sumOf { it.width }

        val minPx = if (minSegmentWidth != Dp.Unspecified) minSegmentWidth.roundToPx() else 0
        val baseWidths = segMeasurables.map { maxOf(it.maxIntrinsicWidth(constraints.maxHeight), minPx) }

        val widths = if (isFixed && constraints.hasBoundedWidth) {
            val leftover = constraints.maxWidth - baseWidths.sum() - totalSeparators
            if (leftover > 0) {
                val extra = leftover / segmentCount
                val remainder = leftover % segmentCount
                baseWidths.mapIndexed { i, w -> w + extra + if (i < remainder) 1 else 0 }
            } else {
                baseWidths
            }
        } else {
            baseWidths
        }

        val segPlaceables = segMeasurables.mapIndexed { i, m ->
            m.measure(constraints.copy(minWidth = widths[i], maxWidth = widths[i]))
        }

        val layoutWidth = (widths.sum() + totalSeparators)
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        val layoutHeight = (segPlaceables + sepPlaceables).maxOf { it.height }
            .coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(layoutWidth, layoutHeight) {
            var x = 0
            segPlaceables.forEachIndexed { i, seg ->
                seg.placeRelative(x, (layoutHeight - seg.height) / 2)
                x += seg.width
                sepPlaceables.getOrNull(i)?.let { sep ->
                    sep.placeRelative(x, (layoutHeight - sep.height) / 2)
                    x += sep.width
                }
            }
        }
    }
}

@Composable
private fun Separator(index: Int, selectedIndex: MutableState<Int>) {
    val selected by selectedIndex
    val alpha by animateFloatAsState(
        targetValue = if (selected == index || selected == index + 1) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "separatorAlpha",
    )

    Box(
        Modifier
            .alpha(alpha)
            .width(SEPARATOR_WIDTH)
            .height(20.dp)
            .background(
                color = TangemTheme.colors2.border.neutral.tertiary.copy(alpha = 0.1f),
            ),
    )
}

private val SEPARATOR_WIDTH = 0.5.dp

@Composable
private fun SegmentSelection(
    itemsWidths: SnapshotStateList<Dp>,
    selectedIndex: Int,
    segmentHeight: Dp,
    separatorWidth: Dp,
) {
    var hasInitiallyMeasured by remember { mutableStateOf(false) }

    val animationSpec: AnimationSpec<Dp> = if (hasInitiallyMeasured) {
        tween(durationMillis = 300)
    } else {
        snap()
    }

    val indicatorOffset by animateDpAsState(
        targetValue = itemsWidths.take(selectedIndex).fold(0.dp, Dp::plus) + separatorWidth * selectedIndex,
        animationSpec = animationSpec,
        label = "indicatorOffset",
    )

    val indicatorWidth by animateDpAsState(
        targetValue = itemsWidths[selectedIndex],
        animationSpec = animationSpec,
        label = "indicatorWidth",
    )

    LaunchedEffect(itemsWidths[selectedIndex] > 0.dp) {
        if (itemsWidths[selectedIndex] > 0.dp) hasInitiallyMeasured = true
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(x = indicatorOffset.roundToPx(), y = 0)
            }
            .size(width = indicatorWidth, height = segmentHeight)
            .shadow(
                elevation = TangemTheme.dimens2.x1,
                shape = RoundedCornerShape(TangemTheme.dimens2.x25),
            )
            .background(
                color = TangemTheme.colors2.tabs.backgroundTertiary,
            ),
    )
}

@Composable
private fun Segment(
    item: TangemSegmentUM,
    index: Int,
    selectedIndex: MutableState<Int>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minSegmentWidth: Dp = Dp.Unspecified,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = minSegmentWidth)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) {
                selectedIndex.value = index
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = item.title.resolveReference(),
            style = TangemTheme.typography2.bodySemibold15,
            color = if (selectedIndex.value == index) {
                TangemTheme.colors2.tabs.textTertiary
            } else {
                TangemTheme.colors2.tabs.textSecondary
            },
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(
                    horizontal = TangemTheme.dimens2.x3,
                    vertical = TangemTheme.dimens2.x2,
                ),
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TangemSegmentedPicker_Preview(@PreviewParameter(PreviewProvider::class) params: TangemSegmentedPickerUM) {
    var selectedItem by remember { mutableStateOf(params.items.first()) }
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors2.surface.level1)
                .padding(TangemTheme.dimens2.x4),
        ) {
            TangemSegmentedPicker(
                isFixed = params.isFixed,
                isAltSurface = params.isAltSurface,
                items = params.items,
                initialSelectedItem = params.items.last(),
                onClick = { selectedItem = it },
            )
        }
    }
}

private class PreviewProvider : PreviewParameterProvider<TangemSegmentedPickerUM> {
    private val items = persistentListOf(
        TangemSegmentUM(id = "1", title = TextReference.Str("Segment")),
        TangemSegmentUM(id = "2", title = TextReference.Str("Segment Two")),
        TangemSegmentUM(id = "3", title = TextReference.Str("Seg 3")),
    )
    override val values: Sequence<TangemSegmentedPickerUM>
        get() = sequenceOf(
            TangemSegmentedPickerUM(
                items = items,
                isFixed = false,
                isAltSurface = false,
            ),
            TangemSegmentedPickerUM(
                items = items,
                isFixed = false,
                isAltSurface = true,
            ),
            TangemSegmentedPickerUM(
                items = items,
                isFixed = true,
                isAltSurface = false,
            ),
            TangemSegmentedPickerUM(
                items = items,
                isFixed = true,
                isAltSurface = true,
            ),
        )
}
// endregion