package com.tangem.features.feed.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItem
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.ds2.tabnavigation.TangemTabNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.dropWhenNoRoom
import com.tangem.features.feed.v2.FeedV2Component
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Slack below which the row is dropped instead of squeezed. A fully collapsed shtorka leaves the feed no
 * room at all, while the semi-open detent leaves a full [FeedV2Component.TabRowHeight].
 */
private val MinRoom = 8.dp

/** One entry of the feed's tab row. */
@Immutable
data class FeedTabUM(val id: String, val title: TextReference)

/**
 * The feed's tab row, shared by every feed surface that pages over [FeedTabSet].
 *
 * The row is deliberately dumb: it renders the tabs it is given and reports taps. It never filters or
 * reorders, and it does not shrink when a tab has nothing to show — the set and its order are the
 * host's, so the search screen's row is identical to the feed home's.
 *
 * It does drop itself when the host offers no room. The row has a fixed height, which a parent's
 * `maxHeight` cannot override, so inside a collapsed sheet it would otherwise paint over the chrome
 * above it.
 *
 * @param tabs every tab of the surface, in host order
 * @param selectedTabIndex index into [tabs]; an out-of-range value simply leaves no tab selected
 * @param onTabSelect tap callback, receiving the index into [tabs]. Side effects a surface wants on
 * top of selection (raising the sheet, for instance) are composed into this lambda by the caller.
 */
@Composable
fun FeedTabRow(
    tabs: ImmutableList<FeedTabUM>,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemTabNavigation(
        tabs = rememberTabItems(tabs = tabs, selectedTabIndex = selectedTabIndex, onTabClick = onTabSelect),
        variant = TangemTabItem.Variant.Transparent,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = modifier
            .dropWhenNoRoom(minRoom = MinRoom)
            .fillMaxWidth()
            .height(FeedV2Component.TabRowHeight),
    )
}

@Composable
private fun rememberTabItems(
    tabs: ImmutableList<FeedTabUM>,
    selectedTabIndex: Int,
    onTabClick: (Int) -> Unit,
): ImmutableList<TangemTabItemUM> {
    val currentOnTabClick by rememberUpdatedState(onTabClick)

    return remember(tabs, selectedTabIndex) {
        tabs
            .mapIndexed { index, tab ->
                TangemTabItemUM.Content(
                    id = tab.id,
                    label = tab.title,
                    isSelected = index == selectedTabIndex,
                    onClick = { currentOnTabClick(index) },
                )
            }
            .toImmutableList()
    }
}