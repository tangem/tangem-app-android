package com.tangem.features.feed.components.search

import androidx.compose.animation.core.EaseOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.core.ui.ds.field.search.TangemFieldShape
import com.tangem.core.ui.ds.field.search.TangemSearchField
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.feed.model.search.SearchModel
import com.tangem.features.feed.ui.search.SearchContent
import com.tangem.features.feed.ui.search.state.SearchCallbacks
import dev.chrisbanes.haze.HazeProgressive

internal class DefaultSearchComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableModularBottomSheetContentComponent, AppComponentContext by appComponentContext {

    private val model = getOrCreateModel<SearchModel, Params>(params = params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

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
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        val state by model.state.collectAsStateWithLifecycle()
        val searchCallbacks = remember {
            SearchCallbacks(
                onLoadMore = model::loadMore,
                onClearHintsClick = model::clearSearchHistory,
                onTextHintClick = model::onTextHintClick,
                onResultMarketTokenClick = model::onResultMarketTokenClick,
                onHistoryTokenClick = model::onHistoryTokenClick,
            )
        }
        SearchContent(
            modifier = modifier,
            content = state.content,
            searchCallbacks = searchCallbacks,
            contentPadding = contentPadding,
        )
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: SearchBottomSheetRoute,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (config) {
        is SearchBottomSheetRoute.TokenSelector -> SearchTokenSelectorComponent(
            context = childByContext(componentContext),
            params = SearchTokenSelectorComponent.Params(
                entries = config.entries,
                appCurrency = config.appCurrency,
                isBalanceHidden = config.isBalanceHidden,
                onTokenSelected = config.onTokenSelected,
                onDismiss = config.onDismiss,
            ),
        )
    }

    data class Params(
        val onBackClick: () -> Unit,
        val onMarketTokenClick: ((TokenMarketParams, AppCurrency) -> Unit),
        val sourceParams: String,
    )
}