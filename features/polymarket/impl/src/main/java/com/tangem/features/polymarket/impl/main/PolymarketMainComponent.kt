package com.tangem.features.polymarket.impl.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.polymarket.impl.main.addfunds.PolymarketAddFundsComponent
import com.tangem.features.polymarket.impl.main.model.PolymarketMainModel
import com.tangem.features.polymarket.impl.main.model.PolymarketMainParams
import com.tangem.features.polymarket.impl.main.ui.PolymarketMainScreen
import com.tangem.features.tokenreceive.TokenReceiveComponent

/**
 * Discovery feed screen.
 *

 * factory — its model is resolved from the model map by [getOrCreateModel].
 *
 * @param userWalletId wallet the feature was opened for; the model reads it back out of its params container,
 *  so it must be handed over here.
 *  container, so it must be handed over here.
 */
internal class PolymarketMainComponent(
    appComponentContext: AppComponentContext,
    private val userWalletId: UserWalletId,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: PolymarketMainModel = getOrCreateModel(
        params = PolymarketMainParams(userWalletId = userWalletId),
    )

    private val addFundsSlot = childSlot(
        source = model.addFundsNavigation,
        serializer = null,
        key = "addFunds",
        handleBackButton = false,
        childFactory = { _, componentContext -> addFundsChild(componentContext) },
    )

    private val receiveSlot = childSlot(
        source = model.receiveNavigation,
        serializer = null,
        key = "receive",
        handleBackButton = false,
        childFactory = ::receiveChild,
    )

    init {
        // Both an app sent to the background and an event opened on top of the feed pause the component, and
        // neither is a moment to keep refreshing what nobody is looking at.
        lifecycle.doOnResume { model.onResume() }
        lifecycle.doOnPause { model.onPause() }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val addFunds by addFundsSlot.subscribeAsState()
        val receive by receiveSlot.subscribeAsState()

        PolymarketMainScreen(
            state = state,
            onBackClick = model::onBackClick,
            onSearchClick = model::onSearchClick,
            onHistoryClick = model::onHistoryClick,
            onLoadMore = model::onLoadMore,
            onVisibleEventsChange = model::onVisibleEventsChange,
            onScrollIdle = model::onScrollIdle,
            onScrollPositionRestore = model::onScrollPositionRestore,
            modifier = modifier,
        )

        addFunds.child?.instance?.BottomSheet()
        receive.child?.instance?.BottomSheet()
    }

    private fun addFundsChild(componentContext: ComponentContext): ComposableBottomSheetComponent =
        PolymarketAddFundsComponent(
            appComponentContext = childByContext(componentContext),
            params = PolymarketAddFundsComponent.Params(callback = model),
        )

    private fun receiveChild(
        config: TokenReceiveConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = tokenReceiveComponentFactory.create(
        context = childByContext(componentContext),
        params = TokenReceiveComponent.Params(
            config = config,
            onDismiss = model.receiveNavigation::dismiss,
        ),
    )
}