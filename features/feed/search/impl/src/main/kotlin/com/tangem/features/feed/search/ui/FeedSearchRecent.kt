package com.tangem.features.feed.search.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_clock_20
import com.tangem.core.ui.res.generated.icons.ic_cross_circle_20_filled
import com.tangem.features.feed.search.ui.state.RecentUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Search history offered while the query is blank: results the user opened before as a horizontal
 * strip, then their latest queries.
 */
@Composable
internal fun FeedSearchRecent(state: RecentUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        RecentHeader(onClearClick = state.onClearClick)

        if (state.items.isNotEmpty()) {
            RecentItems(items = state.items)
        }

        state.queries.forEachIndexed { index, query ->
            RecentQueryRow(query = query, divider = index != state.queries.lastIndex)
        }
    }
}

@Composable
private fun RecentHeader(onClearClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResourceSafe(R.string.feed_search_recent_title),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.secondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResourceSafe(R.string.feed_search_recent_clear),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            modifier = Modifier.clickable(onClick = onClearClick),
        )
    }
}

@Composable
private fun RecentItems(items: ImmutableList<RecentUM.ItemUM>) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(items = items, key = RecentUM.ItemUM::id) { item ->
            RecentItemCell(item = item)
        }
    }
}

@Composable
private fun RecentItemCell(item: RecentUM.ItemUM) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = item.onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TangemTokenIcon(state = item.icon, size = TangemTokenIcon.Size.X40)
        Text(
            text = item.title,
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
        )
    }
}

@Composable
private fun RecentQueryRow(query: RecentUM.QueryUM, divider: Boolean) {
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
            state = RecentUM(
                items = persistentListOf(
                    previewItem(id = "bitcoin", title = "Bitcoin"),
                    previewItem(id = "tether", title = "Tether"),
                    previewItem(id = "world-cup", title = "World Cup Winner"),
                    previewItem(id = "gram", title = "Gram"),
                ),
                queries = persistentListOf(
                    previewQuery(text = "D. Trump"),
                    previewQuery(text = "Apple"),
                    previewQuery(text = "Dollar"),
                ),
                onClearClick = {},
            ),
        )
    }
}

private fun previewItem(id: String, title: String) = RecentUM.ItemUM(
    id = id,
    title = title,
    icon = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = null)),
    onClick = {},
)

private fun previewQuery(text: String) = RecentUM.QueryUM(text = text, onClick = {}, onRemoveClick = {})