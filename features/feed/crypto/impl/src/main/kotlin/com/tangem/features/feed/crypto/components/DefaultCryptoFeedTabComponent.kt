package com.tangem.features.feed.crypto.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.features.feed.crypto.CryptoFeedTabComponent
import com.tangem.features.feed.crypto.model.CryptoFeedTabModel
import com.tangem.features.feed.crypto.ui.CryptoFeedTabContent
import com.tangem.features.promobanners.api.PromoBannersBlockComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class DefaultCryptoFeedTabComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Suppress("UnusedPrivateProperty") @Assisted params: Unit,
    promoBannersBlockComponentFactory: PromoBannersBlockComponent.Factory,
) : CryptoFeedTabComponent, AppComponentContext by context {

    private val model: CryptoFeedTabModel = getOrCreateModel()

    private val promoBannersBlockComponent: PromoBannersBlockComponent = promoBannersBlockComponentFactory.create(
        context = child("promoBannersBlockComponent"),
        params = PromoBannersBlockComponent.Params(
            placeholder = PromoBannersBlockComponent.Placeholder.FEED,
            isInitiallyVisibleOnScreen = false,
        ),
    )

    // owned by the component: the page stays CREATED while unselected, so scroll survives switches
    private val listState = LazyListState()

    @Composable
    override fun Content(bottomSheetState: State<BottomSheetState>, contentPadding: PaddingValues, modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        val sheetState = bottomSheetState.value
        LaunchedEffect(sheetState) {
            model.containerBottomSheetState.value = sheetState
            promoBannersBlockComponent.setVisibleOnScreen(isVisible = sheetState == BottomSheetState.EXPANDED)
        }

        LifecycleStartEffect(Unit) {
            model.isVisibleOnScreen.value = true
            onStopOrDispose {
                model.isVisibleOnScreen.value = false
            }
        }

        CryptoFeedTabContent(
            state = state,
            listState = listState,
            promoBanners = { bannersModifier ->
                promoBannersBlockComponent.ContentWithPadding(
                    horizontalItemPadding = 16.dp,
                    walletId = null,
                    modifier = bannersModifier,
                )
            },
            contentPadding = contentPadding,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : CryptoFeedTabComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultCryptoFeedTabComponent
    }
}