package com.tangem.features.feed.ui.v2

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.ds2.shtorka.ShtorkaExpansion
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.features.feed.search.FeedSearchBarController

/**
 * The feed's search bar, rendered in the host's pinned sheet header. State is shared through
 * [FeedSearchBarController]: focusing expands the shtorka and opens the search screen, typing
 * feeds it the query.
 */
@Composable
internal fun FeedSearchBarHeader(
    searchBarController: FeedSearchBarController,
    sheetExpansion: State<ShtorkaExpansion>,
    onExpandSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchBar by searchBarController.state.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        TangemSearch(
            state = TangemSearch.State(
                placeholderText = resourceReference(R.string.markets_search_title_placeholder),
                query = searchBar.query,
                onQueryChange = searchBarController::onQueryChange,
                isActive = searchBar.isActive,
                onActiveChange = { active ->
                    if (active) {
                        searchBarController.onActiveChange(isActive = true)
                        onExpandSheet()
                    }
                },
                onClearClick = { searchBarController.onQueryChange("") },
                onSubmit = searchBarController::onSubmit,
                onCloseClick = { searchBarController.onActiveChange(false) },
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        if (!searchBar.isActive && sheetExpansion.value != ShtorkaExpansion.FULL) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .testTag(MainScreenTestTags.MARKETS_SHEET_EXPAND_AREA)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onExpandSheet,
                    ),
            )
        }
    }
}