package com.tangem.features.feed.ui.v2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.R
import com.tangem.core.ui.components.rememberIsKeyboardVisible
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.features.feed.search.FeedSearchBarController

/**
 * The feed's search bar, rendered in the host's pinned sheet header. State is shared through
 * [FeedSearchBarController]: focusing expands the shtorka and raises the keyboard over the current
 * feed screen, the first typed character opens the search screen.
 *
 * The bar gives up focus once the keyboard goes away — hidden by a touch in the feed content, by
 * back or by the shtorka leaving full — so it never sits focused without a keyboard.
 */
@Composable
internal fun FeedSearchBarHeader(
    searchBarController: FeedSearchBarController,
    onExpandSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchBar by searchBarController.state.collectAsStateWithLifecycle()
    var isFocused by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val isKeyboardVisible by rememberIsKeyboardVisible()
    LaunchedEffect(isKeyboardVisible) {
        if (!isKeyboardVisible && isFocused) focusManager.clearFocus()
    }

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
                onActiveChange = { isActive ->
                    isFocused = isActive
                    searchBarController.onFocusChange(isActive)
                    if (isActive) onExpandSheet()
                },
                onClearClick = { searchBarController.onQueryChange("") },
                onSubmit = searchBarController::onSubmit,
                onCloseClick = searchBarController::onClose,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(MainScreenTestTags.MARKETS_SHEET_EXPAND_AREA),
        )
    }
}