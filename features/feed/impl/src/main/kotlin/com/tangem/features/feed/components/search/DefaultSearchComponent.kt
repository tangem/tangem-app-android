package com.tangem.features.feed.components.search

import androidx.compose.animation.core.EaseOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.core.ui.ds.field.search.TangemFieldShape
import com.tangem.core.ui.ds.field.search.TangemSearchField
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.features.feed.model.search.SearchModel
import com.tangem.features.feed.ui.search.SearchContent
import com.tangem.features.feed.ui.search.state.SearchCallbacks
import dev.chrisbanes.haze.HazeProgressive

internal class DefaultSearchComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableModularBottomSheetContentComponent, AppComponentContext by appComponentContext {

    private val model = getOrCreateModel<SearchModel, Params>(params = params)

    @Composable
    override fun Title(bottomSheetState: State<BottomSheetState>) {
        val state by model.state.collectAsStateWithLifecycle()
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(bottomSheetState.value) {
            if (bottomSheetState.value == BottomSheetState.EXPANDED) {
                focusRequester.requestFocus()
            }
        }

        TangemTopBar(
            modifier = Modifier.hazeEffectTangem {
                progressive = HazeProgressive.verticalGradient(
                    startIntensity = .55f,
                    endIntensity = 0f,
                    preferPerformance = true,
                    easing = EaseOut,
                )
            },
            type = TangemTopBarType.BottomSheet,
            reserveSlotSpace = false,
            content = {
                TangemSearchField(
                    state = state.searchBar,
                    shape = TangemFieldShape.Circle,
                    focusRequester = focusRequester,
                    modifier = Modifier.weight(1f),
                    enabled = bottomSheetState.value == BottomSheetState.EXPANDED,
                )
            },
        )
    }

    @Composable
    override fun Content(
        bottomSheetState: State<BottomSheetState>,
        contentPadding: PaddingValues,
        modifier: Modifier,
    ) {
        val state by model.state.collectAsStateWithLifecycle()
        val searchCallbacks = remember {
            SearchCallbacks(
                onLoadMore = model::loadMore,
                onClearHintsClick = model::clearSearchHistory,
                onTextHintClick = model::onTextHintClick,
                onResultMarketTokenClick = model::onResultMarketTokenClick,
            )
        }
        SearchContent(
            modifier = modifier,
            content = state.content,
            searchCallbacks = searchCallbacks,
            contentPadding = contentPadding,
        )
    }

    data class Params(
        val onBackClick: () -> Unit,
    )
}