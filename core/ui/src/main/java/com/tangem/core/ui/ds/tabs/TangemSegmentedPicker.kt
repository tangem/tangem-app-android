package com.tangem.core.ui.ds.tabs

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.conditional
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
        hasSeparator = tangemSegmentedPickerUM.hasSeparator,
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
 * @param hasSeparator            Boolean indicating whether there is a separator between segments.
 * @param isFixed                 Boolean indicating whether the picker has a fixed width.
 * @param isAltSurface            Boolean indicating whether to use an alternative surface style.
 * @param onClick                 Lambda function to be invoked when a segment is clicked.
 */
@Composable
fun TangemSegmentedPicker(
    items: ImmutableList<TangemSegmentUM>,
    modifier: Modifier = Modifier,
    initialSelectedItem: TangemSegmentUM? = null,
    hasSeparator: Boolean = false,
    isFixed: Boolean = false,
    isAltSurface: Boolean = false,
    onClick: (TangemSegmentUM) -> Unit,
) {
    if (items.isEmpty() || items.size == 1) return

    val density = LocalDensity.current

    val itemsWidths = remember { mutableStateListOf(*Array(items.size) { 0.dp }) }
    val selectedIndex = remember { mutableStateOf(items.indexOfFirstOrNull { it == initialSelectedItem } ?: 0) }
    val segmentHeight = remember { mutableStateOf(0.dp) }

    val shape = RoundedCornerShape(TangemTheme.dimens2.x25)
    val spacing = if (hasSeparator) {
        TangemTheme.dimens2.x4
    } else {
        TangemTheme.dimens2.x1
    }

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
            selectedIndex = selectedIndex.value,
            segmentHeight = segmentHeight.value,
            spacing = spacing,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            items.fastForEachIndexed { index, item ->
                Segment(
                    item = item,
                    index = index,
                    isFixed = isFixed,
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
            }
        }
    }
}

@Composable
private fun SegmentSelection(itemsWidths: SnapshotStateList<Dp>, selectedIndex: Int, segmentHeight: Dp, spacing: Dp) {
    val indicatorOffset by animateDpAsState(
        targetValue = itemsWidths.take(selectedIndex).fold(0.dp, Dp::plus) + spacing * selectedIndex,
        animationSpec = tween(durationMillis = 300),
        label = "indicatorOffset",
    )

    val indicatorWidth by animateDpAsState(
        targetValue = itemsWidths[selectedIndex],
        animationSpec = tween(durationMillis = 300),
        label = "indicatorWidth",
    )

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
private fun RowScope.Segment(
    item: TangemSegmentUM,
    index: Int,
    isFixed: Boolean,
    selectedIndex: MutableState<Int>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .conditional(isFixed) {
                weight(1f)
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) {
                selectedIndex.value = index
                onClick()
            },
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
                hasSeparator = params.hasSeparator,
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
                hasSeparator = false,
                isFixed = false,
                isAltSurface = false,
            ),
            TangemSegmentedPickerUM(
                items = items,
                hasSeparator = true,
                isFixed = false,
                isAltSurface = true,
            ),
            TangemSegmentedPickerUM(
                items = items,
                hasSeparator = false,
                isFixed = true,
                isAltSurface = false,
            ),
            TangemSegmentedPickerUM(
                items = items,
                hasSeparator = true,
                isFixed = true,
                isAltSurface = true,
            ),
        )
}
// endregion