package com.tangem.features.feed.earn.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioComponent
import com.tangem.features.feed.earn.model.search.EarnFeedSearchTabModel
import com.tangem.features.feed.earn.ui.EarnSearchContent
import com.tangem.features.feed.nav.FeedTabComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow

@Stable
internal class EarnFeedSearchTabComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Params,
    private val addToPortfolioComponentFactory: AddToPortfolioComponent.Factory,
) : FeedTabComponent, AppComponentContext by context {

    private val model: EarnFeedSearchTabModel = getOrCreateModel(params)

    // owned by the component: the page stays CREATED while unselected, so scroll survives switches
    private val listState = LazyListState()

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(bottomSheetState: State<BottomSheetState>, contentPadding: PaddingValues, modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        val sheetState = bottomSheetState.value
        LaunchedEffect(sheetState) {
            model.containerBottomSheetState.value = sheetState
        }

        LifecycleStartEffect(Unit) {
            model.isVisibleOnScreen.value = true
            onStopOrDispose {
                model.isVisibleOnScreen.value = false
            }
        }

        EarnSearchContent(
            state = state,
            listState = listState,
            contentPadding = contentPadding,
            modifier = modifier,
        )
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: EarnBottomSheetRoute.AddToPortfolio,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = addToPortfolioComponentFactory.create(
        context = childByContext(componentContext),
        params = AddToPortfolioComponent.Params(addToPortfolioManager = config.manager),
    )

    data class Params(val query: StateFlow<String>)

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): EarnFeedSearchTabComponent
    }
}