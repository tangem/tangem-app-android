package com.tangem.features.polymarket.impl

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.polymarket.api.PolymarketComponent
import com.tangem.features.polymarket.impl.details.PolymarketEventDetailsComponent
import com.tangem.features.polymarket.impl.entry.PolymarketEntryComponent
import com.tangem.features.polymarket.impl.main.PolymarketMainComponent
import com.tangem.features.polymarket.impl.model.PolymarketModel
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.onboarding.PolymarketOnboardingComponent
import com.tangem.features.polymarket.impl.search.PolymarketSearchComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultPolymarketComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: PolymarketComponent.Params,
    private val portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
) : PolymarketComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<PolymarketRoute>()

    private val detailsSlotNavigation = SlotNavigation<PolymarketRoute.EventDetails>()

    private val stackRouter = InnerRouter<PolymarketRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    // Event details is presented as a modal bottom sheet over the feed (per design), so its route is
    // diverted from the stack into a slot: the feed stays composed and visible behind the sheet's scrim.
    private val innerRouter = object : Router by stackRouter {
        override fun push(route: Route, onComplete: (Boolean) -> Unit) {
            if (route is PolymarketRoute.EventDetails) {
                detailsSlotNavigation.activate(route)
                onComplete(true)
            } else {
                stackRouter.push(route, onComplete)
            }
        }
    }

    /** Router of the details sheet itself: popping it dismisses the slot, anything else goes up as usual. */
    private val detailsRouter = object : Router by stackRouter {
        override fun pop(onComplete: (Boolean) -> Unit) {
            detailsSlotNavigation.dismiss { isSuccess -> onComplete(isSuccess) }
        }
    }

    private val model: PolymarketModel = getOrCreateModel(
        params = params,
        router = innerRouter,
    )

    private val childStack = childStack(
        key = "polymarketStack",
        source = stackNavigation,
        serializer = null,
        initialConfiguration = model.initialRoute,
        handleBackButton = true,
        childFactory = { configuration, factoryContext ->
            getChildComponent(
                configuration = configuration,
                factoryContext = childByContext(
                    componentContext = factoryContext,
                    router = innerRouter,
                ),
            )
        },
    )

    // Declared after the stack so its back handler takes priority while the sheet is shown.
    private val detailsSlot = childSlot(
        key = "polymarketEventDetailsSlot",
        source = detailsSlotNavigation,
        serializer = null,
        handleBackButton = true,
        childFactory = { route, factoryContext ->
            PolymarketEventDetailsComponent(
                appComponentContext = childByContext(
                    componentContext = factoryContext,
                    router = detailsRouter,
                ),
                params = PolymarketEventDetailsComponent.Params(
                    eventId = route.eventId,
                    // The route carries the wallet the feed was opened for; the feature's own params
                    // hold none until the entry gate picks one.
                    userWalletId = route.userWalletId,
                    marketId = route.marketId,
                    assetId = route.assetId,
                ),
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val childStackValue by childStack.subscribeAsState()
        val detailsSlotValue by detailsSlot.subscribeAsState()

        Children(
            stack = childStackValue,
            modifier = modifier,
            animation = stackAnimation { fade() },
        ) { child ->
            child.instance.Content(Modifier.fillMaxSize())
        }

        detailsSlotValue.child?.instance?.BottomSheet()
    }

    private fun getChildComponent(
        configuration: PolymarketRoute,
        factoryContext: AppComponentContext,
    ): ComposableContentComponent = when (configuration) {
        is PolymarketRoute.Entry -> PolymarketEntryComponent(
            appComponentContext = factoryContext,
            params = params,
            portfolioSelectorComponentFactory = portfolioSelectorComponentFactory,
        )
        is PolymarketRoute.Onboarding -> PolymarketOnboardingComponent(
            appComponentContext = factoryContext,
            userWalletId = configuration.userWalletId,
        )
        is PolymarketRoute.Main -> PolymarketMainComponent(
            appComponentContext = factoryContext,
            userWalletId = configuration.userWalletId,
            accessMode = configuration.accessMode,
        )
        is PolymarketRoute.EventDetails -> error("EventDetails is presented as a bottom sheet, not a stack screen")
        is PolymarketRoute.Search -> PolymarketSearchComponent(
            appComponentContext = factoryContext,
        )
    }

    private fun onChildBack() {
        if (childStack.value.backStack.isEmpty()) {
            router.pop()
        } else {
            stackNavigation.pop()
        }
    }

    @AssistedFactory
    interface Factory : PolymarketComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: PolymarketComponent.Params,
        ): DefaultPolymarketComponent
    }
}