package com.tangem.features.yield.supply.impl.entry

import androidx.compose.foundation.layout.Box
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
import com.tangem.features.yield.supply.api.YieldSupplyActiveComponent
import com.tangem.features.yield.supply.api.YieldSupplyEntryComponent
import com.tangem.features.yield.supply.api.YieldSupplyPromoComponent
import com.tangem.features.yield.supply.api.entry.YieldSupplyEntryRoute
import com.tangem.features.yield.supply.impl.entry.model.YieldSupplyEntryModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultYieldSupplyEntryComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: YieldSupplyEntryComponent.Params,
    private val yieldSupplyPromoComponentFactory: YieldSupplyPromoComponent.Factory,
    private val yieldSupplyActiveComponentFactory: YieldSupplyActiveComponent.Factory,
) : YieldSupplyEntryComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<YieldSupplyEntryRoute>()

    private val innerRouter = InnerRouter<YieldSupplyEntryRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val model: YieldSupplyEntryModel = getOrCreateModel(
        params = params,
        router = innerRouter,
    )

    private val childStack = childStack(
        key = "yieldSupplyEntryStack",
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
        configuration: YieldSupplyEntryRoute,
        factoryContext: AppComponentContext,
    ): ComposableContentComponent = when (configuration) {
        is YieldSupplyEntryRoute.Empty -> EmptyComponent
        is YieldSupplyEntryRoute.Promo -> yieldSupplyPromoComponentFactory.create(
            context = factoryContext,
            params = YieldSupplyPromoComponent.Params(
                userWalletId = params.userWalletId,
                currency = configuration.cryptoCurrency,
                apy = configuration.apy,
            ),
        )
        is YieldSupplyEntryRoute.Active -> yieldSupplyActiveComponentFactory.create(
            context = factoryContext,
            params = YieldSupplyActiveComponent.Params(
                userWalletId = params.userWalletId,
                cryptoCurrency = configuration.cryptoCurrency,
            ),
        )
    }

    private object EmptyComponent : ComposableContentComponent {
        @Composable
        override fun Content(modifier: Modifier) {
            Box(modifier = modifier)
        }
    }

    private fun onChildBack() {
        if (childStack.value.backStack.isEmpty() ||
            childStack.value.backStack.last().configuration is YieldSupplyEntryRoute.Empty
        ) {
            router.pop()
        } else {
            stackNavigation.pop()
        }
    }

    @AssistedFactory
    interface Factory : YieldSupplyEntryComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: YieldSupplyEntryComponent.Params,
        ): DefaultYieldSupplyEntryComponent
    }
}