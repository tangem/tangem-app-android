package com.tangem.features.feed.search.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_clock_20
import com.tangem.core.ui.res.generated.icons.ic_cross_circle_20_filled
import com.tangem.features.feed.search.ui.state.RecentQueryUM
import com.tangem.features.feed.ui.FeedSearchSectionHeader
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** Search history offered while the query is blank: the latest queries the user typed. */
@Composable
internal fun FeedSearchRecent(queries: ImmutableList<RecentQueryUM>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        FeedSearchSectionHeader(
            title = stringResourceSafe(R.string.feed_search_recent_title),
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        queries.forEachIndexed { index, query ->
            RecentQueryRow(query = query, divider = index != queries.lastIndex)
        }
    }
}

@Composable
private fun RecentQueryRow(query: RecentQueryUM, divider: Boolean) {
    TangemRow(
        divider = divider,
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        titleSlot = { TangemRowText(text = query.text, role = TangemRowTextRole.Title) },
        startSlot = {
            Icon(
                imageVector = Icons.ic_clock_20,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.primary,
            )
        },
        endSlot = {
            Icon(
                imageVector = Icons.ic_cross_circle_20_filled,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.tertiary,
                modifier = Modifier.clickable(onClick = query.onRemoveClick),
            )
        },
        onClick = query.onClick,
    )
}

@Preview(showBackground = true)
@Composable
private fun FeedSearchRecentPreview() {
    TangemThemePreviewRedesign {
        FeedSearchRecent(
            queries = persistentListOf(
                previewQuery(text = "D. Trump"),
                previewQuery(text = "Apple"),
                previewQuery(text = "Dollar"),
            ),
        )
    }
}

private fun previewQuery(text: String) = RecentQueryUM(text = text, onClick = {}, onRemoveClick = {})