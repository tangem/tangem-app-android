package com.tangem.features.feed.ui.v2

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.feed.search.FeedSearchBarController

/**
 * The feed's search bar, rendered in the host's pinned sheet header. State is shared through
 * [FeedSearchBarController]: focusing expands the shtorka and opens the search screen, typing
 * feeds it the query.
 */
@Composable
internal fun FeedSearchBarHeader(
    searchBarController: FeedSearchBarController,
    onExpandSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchBar by searchBarController.state.collectAsStateWithLifecycle()

    TangemSearch(
        state = TangemSearch.State(
            placeholderText = resourceReference(R.string.markets_search_title_placeholder),
            query = searchBar.query,
            onQueryChange = searchBarController::onQueryChange,
            isActive = searchBar.isActive,
            onActiveChange = { active ->
                searchBarController.onActiveChange(active)
                if (active) onExpandSheet()
            },
            onClearClick = { searchBarController.onQueryChange("") },
            onCloseClick = { searchBarController.onActiveChange(false) },
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )
}