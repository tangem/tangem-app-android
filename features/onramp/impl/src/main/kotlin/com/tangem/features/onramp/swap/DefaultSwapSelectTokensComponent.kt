package com.tangem.features.onramp.swap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioComponent
import com.tangem.features.onramp.component.SwapSelectTokensComponent
import com.tangem.features.onramp.swap.availablepairs.AvailableSwapPairsComponent
import com.tangem.features.onramp.swap.availablepairs.model.AddToPortfolioRoute
import com.tangem.features.onramp.swap.model.SwapSelectTokensModel
import com.tangem.features.onramp.swap.ui.SwapSelectTokens
import com.tangem.features.onramp.tokenlist.OnrampTokenListComponent
import com.tangem.features.onramp.tokenlist.entity.OnrampOperation
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class DefaultSwapSelectTokensComponent @AssistedInject constructor(
    tokenListComponentFactory: OnrampTokenListComponent.Factory,
    availableSwapPairsComponentFactory: AvailableSwapPairsComponent.Factory,
    analyticsEventHandler: AnalyticsEventHandler,
    private val addToPortfolioComponentFactory: AddToPortfolioComponent.Factory,
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: SwapSelectTokensComponent.Params,
) : AppComponentContext by appComponentContext, SwapSelectTokensComponent {

    private val model: SwapSelectTokensModel = getOrCreateModel(params)

    private val selectFromTokenListComponent: OnrampTokenListComponent = tokenListComponentFactory.create(
        context = child(key = "select_from_token_list"),
        params = OnrampTokenListComponent.Params(
            filterOperation = OnrampOperation.SWAP,
            userWalletId = params.userWalletId,
            onTokenClick = model::selectFromToken,
        ),
    )

    private val selectToTokenListComponent: AvailableSwapPairsComponent = availableSwapPairsComponentFactory.create(
        context = child(key = "select_to_token_list"),
        params = AvailableSwapPairsComponent.Params(
            userWalletId = params.userWalletId,
            selectedStatus = model.fromCurrencyStatus,
            onTokenClick = model::selectToToken,
        ),
    )

    private val bottomSheetSlot = childSlot(
        source = selectToTokenListComponent.bottomSheetNavigation,
        serializer = AddToPortfolioRoute.serializer(),
        key = "add_to_portfolio_bottom_sheet",
        handleBackButton = false,
        childFactory = { _, context -> bottomSheetChild(context) },
    )

    init {
        analyticsEventHandler.send(event = MainScreenAnalyticsEvent.SwapScreenOpened())
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun bottomSheetChild(componentContext: ComponentContext): ComposableBottomSheetComponent {
        return addToPortfolioComponentFactory.create(
            context = childByContext(componentContext),
            params = AddToPortfolioComponent.Params(
                addToPortfolioManager = selectToTokenListComponent.addToPortfolioManager!!,
                callback = selectToTokenListComponent.addToPortfolioCallback,
                shouldSkipTokenActionsScreen = true,
            ),
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        val fromTokensState by selectFromTokenListComponent.uiState.collectAsStateWithLifecycle()
        val toTokensState by selectToTokenListComponent.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        SwapSelectTokens(
            state = state,
            selectFromTokenListComponent = selectFromTokenListComponent,
            selectFromTokenListState = fromTokensState,
            selectToTokenListComponent = selectToTokenListComponent,
            selectToTokenListState = toTokensState,
            modifier = modifier,
        )

        bottomSheet.child?.instance?.BottomSheet()
    }

    @AssistedFactory
    interface Factory : SwapSelectTokensComponent.Factory {

        override fun create(
            context: AppComponentContext,
            params: SwapSelectTokensComponent.Params,
        ): DefaultSwapSelectTokensComponent
    }
}