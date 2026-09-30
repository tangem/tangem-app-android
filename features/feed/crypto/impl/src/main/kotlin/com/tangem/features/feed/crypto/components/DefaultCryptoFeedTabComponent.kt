package com.tangem.features.feed.crypto.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.feed.crypto.CryptoFeedTabComponent
import com.tangem.features.feed.crypto.model.CryptoFeedTabModel
import com.tangem.features.feed.crypto.model.entity.FeedCryptoTabBottomSheetConfig
import com.tangem.features.feed.crypto.ui.CryptoFeedTabContent
import com.tangem.features.feed.crypto.ui.MarketIndexInfoBottomSheet
import com.tangem.features.feed.crypto.ui.MarketIndexInfoBottomSheetContent
import com.tangem.features.feed.crypto.ui.MarketInfoBottomSheet
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
            placeholder = PromoBannersBlockComponent.Placeholder.CRYPTO,
            isInitiallyVisibleOnScreen = false,
        ),
    )

    // owned by the component: the page stays CREATED while unselected, so scroll survives switches
    private val listState = LazyListState()

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = { config, _ ->
            bottomSheetChild(config)
        },
    )

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

        val bottomSheet by bottomSheetSlot.subscribeAsState()
        bottomSheet.child?.instance?.BottomSheet()
    }

    override fun resetScroll() {
        listState.requestScrollToItem(index = 0)
    }

    private fun bottomSheetChild(config: FeedCryptoTabBottomSheetConfig): ComposableBottomSheetComponent {
        return when (config) {
            is FeedCryptoTabBottomSheetConfig.MarketIndexInfo -> indexInfoChild(config = config)
            FeedCryptoTabBottomSheetConfig.MarketInfo -> infoChild()
        }
    }

    private fun indexInfoChild(config: FeedCryptoTabBottomSheetConfig.MarketIndexInfo): ComposableBottomSheetComponent =
        object : ComposableBottomSheetComponent {
            override fun dismiss() = model.bottomSheetNavigation.dismiss()

            @Composable
            override fun BottomSheet() {
                MarketIndexInfoBottomSheet(
                    state = MarketIndexInfoBottomSheetContent(config.data),
                    onLinkClick = model::openUrl,
                    onDismiss = ::dismiss,
                )
            }
        }

    private fun infoChild(): ComposableBottomSheetComponent = object : ComposableBottomSheetComponent {
        override fun dismiss() = model.bottomSheetNavigation.dismiss()

        @Composable
        override fun BottomSheet() {
            MarketInfoBottomSheet(
                onLinkClick = model::openUrl,
                onDismiss = ::dismiss,
            )
        }
    }

    @AssistedFactory
    interface Factory : CryptoFeedTabComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultCryptoFeedTabComponent
    }
}