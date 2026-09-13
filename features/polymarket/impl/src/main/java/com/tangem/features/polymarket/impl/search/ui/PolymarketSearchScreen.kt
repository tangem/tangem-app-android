package com.tangem.features.polymarket.impl.search.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.haze.ProvideHaze
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_search_20
import com.tangem.features.polymarket.impl.common.ui.PolymarketLoadMoreEffect
import com.tangem.features.polymarket.impl.common.ui.PolymarketLoadingState
import com.tangem.features.polymarket.impl.common.ui.PolymarketNextPageLoader
import com.tangem.features.polymarket.impl.common.ui.PolymarketReloadPrompt
import com.tangem.features.polymarket.impl.common.ui.PolymarketSearchBarClearance
import com.tangem.features.polymarket.impl.main.ui.PolymarketEventCard
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketEventRowUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketEventUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketOutcomeUM
import com.tangem.features.polymarket.impl.search.ui.state.PolymarketSearchUM
import kotlinx.collections.immutable.persistentListOf

private const val KEY_RESULTS_HEADER = "results_header"
private const val KEY_STATUS = "status"

/**
 * Search over discoverable events. The field sits at the bottom, right above the keyboard, and takes
 * focus as the screen opens — the keyboard is the primary tool here.
 */
@Composable
internal fun PolymarketSearchScreen(state: PolymarketSearchUM, onLoadMore: () -> Unit, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // The feature lives inside a modal — a separate window whose LocalHazeState belongs to the root
    // window, where haze cannot sample from here. A local provider keeps the source and the glass of
    // this screen in one window.
    ProvideHaze {
        SearchLayout(state = state, onLoadMore = onLoadMore, focusRequester = focusRequester, modifier = modifier)
    }
}

@Composable
private fun SearchLayout(
    state: PolymarketSearchUM,
    onLoadMore: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // The haze source of the search field's glass; the background is drawn inside the source.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceTangem()
                .background(TangemTheme.colors3.bg.primary),
        ) {
            when (state.content) {
                is PolymarketSearchUM.ContentUM.Initial -> CenterPrompt(
                    title = resourceReference(R.string.prediction_search_initial_title),
                    subtitle = resourceReference(R.string.prediction_search_initial_subtitle),
                )
                is PolymarketSearchUM.ContentUM.Loading -> PolymarketLoadingState(
                    modifier = Modifier.align(Alignment.Center),
                )
                is PolymarketSearchUM.ContentUM.NothingFound -> CenterPrompt(
                    title = resourceReference(R.string.prediction_search_empty_title),
                    subtitle = resourceReference(R.string.prediction_search_empty_subtitle),
                )
                is PolymarketSearchUM.ContentUM.Error -> PolymarketReloadPrompt(
                    modifier = Modifier.align(Alignment.Center),
                    text = resourceReference(R.string.prediction_main_events_load_error),
                    onReloadClick = state.content.onReloadClick,
                )
                is PolymarketSearchUM.ContentUM.Results -> ResultsList(
                    content = state.content,
                    onLoadMore = onLoadMore,
                )
            }
        }

        SearchField(
            state = state,
            focusRequester = focusRequester,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun SearchField(state: PolymarketSearchUM, focusRequester: FocusRequester, modifier: Modifier = Modifier) {
    // The DS component shows its close button while the field is active or filled; the field starts
    // focused, so the button is there from the first frame.
    var isActive by remember { mutableStateOf(true) }

    TangemSearch(
        state = TangemSearch.State(
            placeholderText = resourceReference(R.string.common_search),
            query = state.query,
            onQueryChange = state.onQueryChange,
            isActive = isActive,
            onActiveChange = { isActive = it },
            onClearClick = { state.onQueryChange("") },
            onCloseClick = state.onCloseClick,
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        focusRequester = focusRequester,
    )
}

@Composable
private fun ResultsList(content: PolymarketSearchUM.ContentUM.Results, onLoadMore: () -> Unit) {
    val listState = rememberLazyListState()

    PolymarketLoadMoreEffect(listState = listState, onLoadMore = onLoadMore)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        // The results scroll under the search field; the clearance keeps the last card visible.
        contentPadding = PaddingValues(top = 8.dp, bottom = PolymarketSearchBarClearance),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = KEY_RESULTS_HEADER) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                text = stringResourceSafe(R.string.prediction_search_results_title),
                color = TangemTheme.colors3.text.tertiary,
                style = TangemTheme.typography3.body.medium,
            )
        }

        items(items = content.events, key = { it.id }) { event ->
            PolymarketEventCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = event,
            )
        }

        if (content.isLoadingNextPage) {
            item(key = KEY_STATUS) {
                PolymarketNextPageLoader()
            }
        }
    }
}

@Composable
private fun CenterPrompt(title: TextReference, subtitle: TextReference) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(TangemTheme.colors3.bg.secondary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.ic_search_20),
                contentDescription = null,
                tint = TangemTheme.colors3.icon.secondary,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title.resolveReference(),
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.subheading.medium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle.resolveReference(),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.caption.medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(name = "Initial Light", showBackground = true, widthDp = 360, heightDp = 640)
@Preview(
    name = "Initial Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PolymarketSearchScreenInitialPreview() {
    TangemThemePreviewRedesign {
        PolymarketSearchScreen(
            state = previewState(content = PolymarketSearchUM.ContentUM.Initial),
            onLoadMore = {},
        )
    }
}

@Preview(name = "Results Light", showBackground = true, widthDp = 360, heightDp = 640)
@Preview(
    name = "Results Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PolymarketSearchScreenResultsPreview() {
    TangemThemePreviewRedesign {
        PolymarketSearchScreen(
            state = previewState(
                content = PolymarketSearchUM.ContentUM.Results(
                    events = persistentListOf(previewEvent()),
                    isLoadingNextPage = false,
                ),
            ),
            onLoadMore = {},
        )
    }
}

@Preview(name = "Nothing Light", showBackground = true, widthDp = 360, heightDp = 640)
@Preview(
    name = "Nothing Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PolymarketSearchScreenNothingFoundPreview() {
    TangemThemePreviewRedesign {
        PolymarketSearchScreen(
            state = previewState(content = PolymarketSearchUM.ContentUM.NothingFound, query = "Saburo Arasaka"),
            onLoadMore = {},
        )
    }
}

private fun previewState(content: PolymarketSearchUM.ContentUM, query: String = ""): PolymarketSearchUM =
    PolymarketSearchUM(
        query = query,
        onQueryChange = {},
        onCloseClick = {},
        content = content,
    )

private fun previewEvent(): PolymarketEventUM = PolymarketEventUM(
    id = "uzbekistan",
    title = stringReference("Will Uzbekistan win the 2026 FIFA World Cup?"),
    iconUrl = null,
    volume = stringReference("$6.3M"),
    rows = persistentListOf(
        PolymarketEventRowUM(
            marketId = "probability",
            title = stringReference("Probability"),
            probability = stringReference("80%"),
            outcomes = persistentListOf(
                PolymarketOutcomeUM(assetId = "yes", title = stringReference("Yes"), onClick = {}),
                PolymarketOutcomeUM(assetId = "no", title = stringReference("No"), onClick = {}),
            ),
        ),
    ),
    hiddenMarketsCount = 0,
    onClick = {},
)