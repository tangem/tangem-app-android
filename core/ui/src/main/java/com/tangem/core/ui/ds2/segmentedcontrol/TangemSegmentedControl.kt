package com.tangem.core.ui.ds2.segmentedcontrol

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Design-system v2 segmented control — a pill-shaped track of mutually exclusive segments sharing one
 * animated selection pill.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=7908-343)
 *
 * Behavior notes:
 * - Segments always share the available width equally: by default the control hugs its content and
 *   every segment is as wide as the widest label; pass `Modifier.fillMaxWidth()` to stretch the
 *   control while keeping the segments equal.
 * - Selecting a segment slides the pill to it with a stretch-and-settle motion; the label of the
 *   newly selected segment lights up only once the pill has arrived.
 * - [TangemSegmentedControl.State.Loading] replaces the control with a single pill-shaped shimmer.
 *
 * @param state Segments or the loading placeholder. See [TangemSegmentedControl.State].
 * @param modifier Modifier applied to the control's root. The height is intrinsic; set the width here
 * (hugs content by default). In the loading state the shimmer falls back to a default width unless
 * the modifier specifies one.
 */
@Composable
fun TangemSegmentedControl(state: TangemSegmentedControl.State, modifier: Modifier = Modifier) {
    when (state) {
        is TangemSegmentedControl.State.Loading -> {
            TangemShimmer(
                modifier = modifier
                    .size(width = ShimmerWidth, height = ControlHeight)
                    .clearAndSetSemantics {},
                radius = PillRadius,
            )
        }
        is TangemSegmentedControl.State.Content -> {
            SegmentedControlContent(items = state.items, modifier = modifier)
        }
    }
}

/**
 * Convenience overload of [TangemSegmentedControl] for the common non-loading case.
 *
 * @param items Segments of the control. See [TangemSegmentedControl.Item].
 * @param modifier Modifier applied to the control's root. See the base overload.
 */
@Composable
fun TangemSegmentedControl(items: ImmutableList<TangemSegmentedControl.Item>, modifier: Modifier = Modifier) {
    TangemSegmentedControl(state = TangemSegmentedControl.State.Content(items = items), modifier = modifier)
}

object TangemSegmentedControl {

    /**
     * State of the segmented control.
     *
     * - [Content] — a row of selectable segments. Exactly one segment is expected to have
     *   [Item.isSelected] set; selection is owned by the caller and driven back through
     *   [Item.onClick].
     * - [Loading] — the whole control is replaced by a single pill-shaped shimmer.
     */
    @Immutable
    sealed interface State {

        @Immutable
        data class Content(val items: ImmutableList<Item>) : State

        data object Loading : State
    }

    /**
     * A single segment of [State.Content].
     *
     * @param id Stable identity of the segment, used to key measurements and animations.
     * @param label Text of the segment.
     * @param isSelected Whether the segment is the selected one. Drives the label color and the
     * shared selection pill position.
     * @param onClick Invoked when the segment is tapped, including taps on the already selected
     * segment.
     */
    @Immutable
    data class Item(
        val id: String,
        val label: TextReference,
        val isSelected: Boolean = false,
        val onClick: () -> Unit,
    )
}

@Composable
private fun SegmentedControlContent(items: ImmutableList<TangemSegmentedControl.Item>, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val itemWidths = remember { mutableStateMapOf<String, Dp>() }

    val selectedId = items.firstOrNull { it.isSelected }?.id
    val pillBounds by remember(items) {
        derivedStateOf { resolvePillBounds(items = items, widths = itemWidths) }
    }

    Box(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .clip(CircleShape)
            .background(TangemTheme.colors3.bg.opaque.primary)
            .padding(ContainerPadding),
    ) {
        pillBounds?.let { bounds ->
            SelectionPill(bounds = bounds, selectionKey = selectedId)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(ItemSpacing),
        ) {
            items.forEach { item ->
                SegmentedItem(
                    item = item,
                    modifier = Modifier
                        .weight(1f)
                        .onSizeChanged { size ->
                            itemWidths[item.id] = with(density) { size.width.toDp() }
                        },
                )
            }
        }
    }
}

// region Previews

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemSegmentedControlPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            PreviewSection(title = "Hug") {
                PreviewControl()
            }
            PreviewSection(title = "Fill width") {
                PreviewControl(modifier = Modifier.fillMaxWidth())
            }
            PreviewSection(title = "Loading") {
                TangemSegmentedControl(state = TangemSegmentedControl.State.Loading)
            }
        }
    }
}

@Composable
private fun PreviewSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.body.medium,
        )
        content()
    }
}

/** Holds its own selection so the pill actually animates in interactive preview. */
@Composable
private fun PreviewControl(modifier: Modifier = Modifier) {
    var selectedId by remember { mutableStateOf(PREVIEW_LABELS.first()) }
    val items = remember(selectedId) {
        PREVIEW_LABELS
            .map { label ->
                TangemSegmentedControl.Item(
                    id = label,
                    label = stringReference(label),
                    isSelected = label == selectedId,
                    onClick = { selectedId = label },
                )
            }
            .toImmutableList()
    }
    TangemSegmentedControl(items = items, modifier = modifier)
}

private val PREVIEW_LABELS = listOf("Buy", "Sell", "Swap")

// endregion