package com.tangem.features.tangempay.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.navigation.TangemPayDetailsInnerRoute
import com.tangem.features.tokenreceive.TokenReceiveComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class DefaultTangemPayDetailsContainerComponent @AssistedInject constructor(
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: TangemPayDetailsContainerComponent.Params,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
) : AppComponentContext by appComponentContext, TangemPayDetailsContainerComponent {

    private val stackNavigation = StackNavigation<TangemPayDetailsInnerRoute>()

    private val innerRouter = InnerRouter<TangemPayDetailsInnerRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val childStack = childStack(
        key = "tangemPayDetailsInnerStack",
        source = stackNavigation,
        serializer = TangemPayDetailsInnerRoute.serializer(),
        initialConfiguration = TangemPayDetailsInnerRoute.Details,
        childFactory = ::screenChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val childStack by childStack.subscribeAsState()

        BackHandler(onBack = router::pop)
        Children(stack = childStack, animation = stackAnimation()) { child ->
            child.instance.Content(modifier = modifier)
        }
    }

    private fun screenChild(
        config: TangemPayDetailsInnerRoute,
        componentContext: ComponentContext,
    ): ComposableContentComponent = when (config) {
        TangemPayDetailsInnerRoute.Details -> TangemPayDetailsComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = params,
            tokenReceiveComponentFactory = tokenReceiveComponentFactory,
        )
        TangemPayDetailsInnerRoute.ChangePIN -> TangemPayChangePinComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
        )
        TangemPayDetailsInnerRoute.ChangePINSuccess -> TangemPayChangePinSuccessComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
        )
        TangemPayDetailsInnerRoute.AddToWallet -> TangemPayAddToWalletComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = params,
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
    interface Factory : TangemPayDetailsContainerComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayDetailsContainerComponent.Params,
        ): DefaultTangemPayDetailsContainerComponent
    }
}