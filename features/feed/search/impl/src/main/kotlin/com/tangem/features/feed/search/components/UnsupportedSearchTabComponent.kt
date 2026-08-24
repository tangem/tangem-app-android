package com.tangem.features.feed.search.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.nav.FeedTabComponent
import com.tangem.features.feed.nav.FeedTabId

/**
 * Page shown for a tab whose contributor does not implement `createSearchTab`. The tab keeps its slot
 * in the row — the row mirrors the feed's and never shrinks — so the page has to say something.
 */
@Stable
internal class UnsupportedSearchTabComponent(
    context: AppComponentContext,
    private val tabId: FeedTabId,
) : FeedTabComponent, AppComponentContext by context {

    @Composable
    override fun Content(
        bottomSheetState: State<BottomSheetState>,
        contentPadding: PaddingValues,
        modifier: Modifier,
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            // TODO: [TWI-1608] every tab searches by release, so this page is temporary and unlocalised
            Text(
                text = "Search is not available in ${tabId.value} yet",
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 32.dp),
            )
        }
    }
}