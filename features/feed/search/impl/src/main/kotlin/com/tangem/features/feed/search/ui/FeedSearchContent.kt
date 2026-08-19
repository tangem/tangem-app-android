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
import com.tangem.features.feed.search.ui.state.FeedSearchUM

/**
 * Search results below the host's pinned search bar. The screen renders no search field of its
 * own — the query arrives through the shared search bar state.
 */
@Composable
internal fun FeedSearchContent(state: FeedSearchUM, contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding())
            .padding(horizontal = 16.dp),
    ) {
        // TODO: [TWI-1608] real search results; placeholder echoes the shared query until then
        Text(
            text = if (state.query.isEmpty()) "Start typing to search" else "Searching for “${state.query}”…",
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
        )
    }
}