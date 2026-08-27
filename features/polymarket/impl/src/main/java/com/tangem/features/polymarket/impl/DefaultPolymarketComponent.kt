package com.tangem.features.polymarket.impl

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.polymarket.api.PolymarketComponent
import com.tangem.features.polymarket.impl.details.PolymarketEventDetailsComponent
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
) : PolymarketComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<PolymarketRoute>()

    private val innerRouter = InnerRouter<PolymarketRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

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

    @Composable
    override fun Content(modifier: Modifier) {
        val childStackValue by childStack.subscribeAsState()

        Children(
            stack = childStackValue,
            modifier = modifier,
            animation = stackAnimation { fade() },
        ) { child ->
            child.instance.Content(Modifier.fillMaxSize())
        }
    }

    private fun getChildComponent(
        configuration: PolymarketRoute,
        factoryContext: AppComponentContext,
    ): ComposableContentComponent = when (configuration) {
        is PolymarketRoute.Onboarding -> PolymarketOnboardingComponent(
            appComponentContext = factoryContext,
            params = params,
        )
        is PolymarketRoute.Main -> PolymarketMainComponent(
            appComponentContext = factoryContext,
            userWalletId = params.userWalletId,
            accessMode = configuration.accessMode,
        )
        is PolymarketRoute.EventDetails -> PolymarketEventDetailsComponent(
            appComponentContext = factoryContext,
            eventId = configuration.eventId,
            userWalletId = params.userWalletId,
            marketId = configuration.marketId,
            assetId = configuration.assetId,
        )
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