package com.tangem.core.ui.ds2.tabnavigation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Design-system v2 tab navigation — a horizontally scrollable row of [TangemTabItem]s sharing one
 * animated selection pill.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=6590-2239)
 *
 * Behavior notes:
 * - Selection lives on the tabs themselves ([TangemTabItemUM.Content.isSelected]) and is controlled:
 *   nothing moves until the caller reacts to a tab's `onClick` by passing back a list whose selected
 *   tab has changed.
 * - Tabs are mutually exclusive, so the **first** selected tab takes the pill. If none is selected,
 *   or the selected one is still [TangemTabItemUM.Loading], the row renders without a pill.
 * - The pill travels and resizes to the newly selected tab over 400ms, overshooting slightly, while
 *   stretching to 110% of its width mid-flight.
 * - The selected tab is scrolled into view when it isn't fully visible; a tab already on screen never
 *   triggers a scroll.
 *
 * Figma specifies a minimum of two tabs.
 */
@Composable
fun TangemTabNavigation(
    tabs: ImmutableList<TangemTabItemUM>,
    modifier: Modifier = Modifier,
    variant: TangemTabItem.Variant = TangemTabItem.Variant.Material,
    contentPadding: PaddingValues = PaddingValues(horizontal = ContentHorizontalPadding),
    scrollState: ScrollState = rememberScrollState(),
) {
    val density = LocalDensity.current
    val itemWidths = remember { mutableStateMapOf<String, Dp>() }

    val selectedIndex = remember(tabs) {
        tabs.indexOfFirst { it is TangemTabItemUM.Content && it.isSelected }
    }
    val selectedTabId = tabs.getOrNull(selectedIndex)?.id

    val pillBounds by remember(tabs, selectedIndex) {
        derivedStateOf { resolvePillBounds(tabs = tabs, selectedIndex = selectedIndex, widths = itemWidths) }
    }

    AutoScrollToSelected(
        scrollState = scrollState,
        bounds = pillBounds,
        margin = ContentHorizontalPadding,
    )

    ProvideTangemShimmer {
        Box(
            modifier = modifier
                .horizontalScroll(scrollState)
                .padding(contentPadding),
        ) {
            pillBounds?.let { bounds ->
                SelectionPill(bounds = bounds, selectionKey = selectedTabId, variant = variant)
            }

            Row(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(ItemSpacing),
            ) {
                tabs.forEach { tab ->
                    TangemTabItem(
                        state = tab,
                        modifier = Modifier.onSizeChanged { size ->
                            itemWidths[tab.id] = with(density) { size.width.toDp() }
                        },
                        variant = variant,
                        background = TangemTabItem.Background.Hoisted,
                    )
                }
            }
        }
    }
}

/**
 * Offset and width of the pill, derived from the measured tab widths.
 *
 * Returns `null` until every tab preceding the selected one has been measured: a single unmeasured
 * predecessor makes the whole offset wrong, and one frame without a pill beats one frame with it in
 * the wrong place.
 */
private fun resolvePillBounds(
    tabs: ImmutableList<TangemTabItemUM>,
    selectedIndex: Int,
    widths: Map<String, Dp>,
): PillBounds? {
    val selected = tabs.getOrNull(selectedIndex)
    if (selected !is TangemTabItemUM.Content) return null

    var offset = 0.dp
    for (i in 0 until selectedIndex) {
        offset += (widths[tabs[i].id] ?: return null) + ItemSpacing
    }
    return PillBounds(offset = offset, width = widths[selected.id] ?: return null)
}

// region Previews

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemTabNavigationPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TangemTabItem.Variant.entries.forEach { variant ->
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = variant.name,
                    color = TangemTheme.colors3.text.secondary,
                    style = TangemTheme.typography3.body.medium,
                )
                PreviewRow(variant = variant)
            }
        }
    }
}

/** Holds its own selection so the pill actually animates in interactive preview. */
@Composable
private fun PreviewRow(variant: TangemTabItem.Variant) {
    var selectedTabId by remember { mutableStateOf(PREVIEW_LABELS.first()) }
    val tabs = remember(selectedTabId) {
        PREVIEW_LABELS
            .map<String, TangemTabItemUM> { label ->
                TangemTabItemUM.Content(
                    id = label,
                    label = stringReference(label),
                    isSelected = label == selectedTabId,
                    onClick = { selectedTabId = label },
                )
            }
            .toImmutableList()
    }
    TangemTabNavigation(tabs = tabs, variant = variant)
}

private val PREVIEW_LABELS = listOf("All", "ETFs", "Funds", "Market", "Staking", "Others")

// endregion