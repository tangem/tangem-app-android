package com.tangem.features.earn.impl

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
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.components.haze.ProvideHaze
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.earn.api.EarnEntryComponent
import com.tangem.features.earn.flow.api.EarnFlowComponent
import com.tangem.features.earn.impl.flow.EarnFlowExitHandler
import com.tangem.features.earn.impl.flow.EarnFlowParamsFactory
import com.tangem.features.earn.impl.info.EarnInfoComponent
import com.tangem.features.earn.impl.picker.EarnYieldPickerComponent
import com.tangem.features.earn.product.api.navigation.EarnFlowRoute
import com.tangem.features.earn.product.api.navigation.EarnInfoRoute
import com.tangem.features.earn.product.api.navigation.EarnPickerRoute
import com.tangem.features.earn.product.api.navigation.EarnRoute
import com.tangem.features.earn.product.api.session.EarnFlowExit
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class DefaultEarnEntryComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: EarnEntryComponent.Params,
    private val pickerComponentFactory: EarnYieldPickerComponent.Factory,
    private val infoComponentFactory: EarnInfoComponent.Factory,
    private val flowComponentFactory: EarnFlowComponent.Factory,
    private val flowParamsFactory: EarnFlowParamsFactory,
) : EarnEntryComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<EarnRoute>()

    private val innerRouter = InnerRouter<EarnRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val flowExitHandler = EarnFlowExitHandler(innerRouter = innerRouter, hostRouter = router, entry = params)

    private val childStack = childStack(
        key = "earnEntryStack",
        source = stackNavigation,
        serializer = null,
        initialConfiguration = params.start.toInitialRoute(),
        handleBackButton = true,
        childFactory = { route, factoryContext ->
            createChild(
                route = route,
                childContext = childByContext(componentContext = factoryContext, router = innerRouter),
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
            // Screens animate together: a shared haze state would blur the screen underneath into this one
            ProvideHaze {
                child.instance.Content(Modifier.fillMaxSize())
            }
        }
    }

    private fun createChild(route: EarnRoute, childContext: AppComponentContext): ComposableContentComponent {
        return when (route) {
            is EarnPickerRoute -> pickerComponentFactory.create(
                context = childContext,
                params = EarnYieldPickerComponent.Params(
                    userWalletId = params.userWalletId,
                    cryptoCurrency = params.cryptoCurrency,
                    source = params.source,
                    preselected = route.preselected,
                    shouldRedirectToActive = route.shouldRedirectToActive,
                ),
            )
            is EarnInfoRoute -> infoComponentFactory.create(
                context = childContext,
                params = EarnInfoComponent.Params(
                    userWalletId = params.userWalletId,
                    cryptoCurrency = params.cryptoCurrency,
                    productId = route.productId,
                    source = params.source,
                ),
            )
            is EarnFlowRoute -> createFlowChild(route = route, childContext = childContext)
        }
    }

    private fun createFlowChild(route: EarnFlowRoute, childContext: AppComponentContext): ComposableContentComponent {
        val callback = object : EarnFlowComponent.ModelCallback {
            override fun onFinished(exit: EarnFlowExit) = flowExitHandler.onExit(route.productId, exit)
        }
        val flowParams = flowParamsFactory.create(route = route, entry = params, callback = callback)

        if (flowParams == null) {
            TangemLogger.e("Earn flow: product ${route.productId.value} is not registered")
            // Posted: the stack must not navigate while it is still creating this child
            componentScope.launch(dispatchers.main) { innerRouter.pop() }
            return ComposableContentComponent.EMPTY
        }

        return flowComponentFactory.create(context = childContext, params = flowParams)
    }

    private fun onChildBack() {
        if (childStack.value.backStack.isEmpty()) router.pop() else stackNavigation.pop()
    }

    @AssistedFactory
    interface Factory : EarnEntryComponent.Factory {
        override fun create(context: AppComponentContext, params: EarnEntryComponent.Params): DefaultEarnEntryComponent
    }
}