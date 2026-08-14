package com.tangem.features.feed.ui.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme

/**
 * Placeholder rows for a feed tab whose real list is not implemented yet. TODO: [TWI-1608]
 *
 * @param listState hoisted to the tab component so scroll survives tab switches
 * @param contentPadding insets of the feed chrome pinned above the list
 * @param onItemClick `null` → rows are not clickable
 */
@Composable
internal fun FeedTabPlaceholderList(
    tabId: String,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onItemClick: (() -> Unit)? = null,
) {
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding(),
            bottom = bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        items(count = PLACEHOLDER_ITEMS_COUNT, key = { it }) { index ->
            PlaceholderRow(tabId = tabId, index = index, onClick = onItemClick)
        }
    }
}

@Composable
private fun PlaceholderRow(tabId: String, index: Int, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(size = 12.dp))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(size = 40.dp)
                .background(color = TangemTheme.colors3.bg.tertiary, shape = CircleShape),
        )
        Column(verticalArrangement = Arrangement.spacedBy(space = 2.dp)) {
            Text(
                text = "${tabId.replaceFirstChar(Char::uppercase)} item ${index + 1}",
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                text = "Placeholder",
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
    }
}

private const val PLACEHOLDER_ITEMS_COUNT = 200