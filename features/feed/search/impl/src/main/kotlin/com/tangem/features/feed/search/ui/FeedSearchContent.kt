package com.tangem.features.feed.search.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.FeedTabRow
import com.tangem.features.feed.ui.FeedTabUM
import kotlinx.collections.immutable.ImmutableList

/**
 * Shown while the query is blank. There is no tab row in this state — searching has not started, so
 * there is no scope to pick.
 */
@Composable
internal fun FeedSearchPlaceholder(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding())
            .padding(horizontal = 16.dp),
    ) {
        // TODO: [TWI-1608] Suggested + Popular; recent searches are a separate task
        Text(
            text = "Start typing to search",
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
        )
    }
}

/**
 * Search results: the feed's tab row over the selected tab's own list. The row is the same set and
 * order as the feed home's and does not shrink when a tab finds nothing.
 *
 * @param tabContent the selected tab page, provided by the search screen's pager
 */
@Composable
internal fun FeedSearchTabsContent(
    tabs: ImmutableList<FeedTabUM>,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    tabContent: @Composable (Modifier) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        FeedTabRow(tabs = tabs, selectedTabIndex = selectedTabIndex, onTabSelect = onTabSelect)
        tabContent(Modifier.fillMaxSize())
    }
}