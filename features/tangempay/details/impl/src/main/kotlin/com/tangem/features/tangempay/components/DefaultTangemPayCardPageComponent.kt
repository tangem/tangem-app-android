package com.tangem.features.tangempay.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.limit.setup.TangemPayCardLimitSetupComponent
import com.tangem.features.tangempay.limit.setup.TangemPayCardLimitSetupSuccessComponent
import com.tangem.features.tangempay.navigation.TangemPayDetailsInnerRoute
import com.tangem.features.tokenreceive.TokenReceiveComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayCardPageComponent @AssistedInject constructor(
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: TangemPayCardPageComponent.Params,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
) : AppComponentContext by appComponentContext, TangemPayCardPageComponent {

    private val stackNavigation = StackNavigation<TangemPayDetailsInnerRoute>()

    private val innerRouter = InnerRouter<TangemPayDetailsInnerRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val childStack = childStack(
        key = "tangemPayCardPageInnerStack",
        source = stackNavigation,
        serializer = TangemPayDetailsInnerRoute.serializer(),
        initialConfiguration = TangemPayDetailsInnerRoute.Details,
        childFactory = ::screenChild,
    )

    @Suppress("ReusedModifierInstance")
    @Composable
    override fun Content(modifier: Modifier) {
        val childStack by childStack.subscribeAsState()
        Children(
            stack = childStack,
        ) { child ->
            child.instance.Content(modifier = modifier)
        }
    }

    private fun screenChild(
        config: TangemPayDetailsInnerRoute,
        componentContext: ComponentContext,
    ): ComposableContentComponent = when (config) {
        TangemPayDetailsInnerRoute.Details -> TangemPayCardPageScreenComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = params,
            tokenReceiveComponentFactory = tokenReceiveComponentFactory,
        )
        TangemPayDetailsInnerRoute.ChangePIN -> TangemPayChangePinComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayDetailsContainerComponent.Params(
                userWalletId = params.userWalletId,
                config = params.config,
            ),
        )
        TangemPayDetailsInnerRoute.ChangePINSuccess -> TangemPayChangePinSuccessComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
        )
        TangemPayDetailsInnerRoute.AddToWallet -> TangemPayAddToWalletComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayDetailsContainerComponent.Params(
                userWalletId = params.userWalletId,
                config = params.config,
            ),
        )
        TangemPayDetailsInnerRoute.EditCardDisplayName -> TangemPayEditDisplayNameComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayDetailsContainerComponent.Params(
                userWalletId = params.userWalletId,
                config = params.config,
            ),
        )
        TangemPayDetailsInnerRoute.LimitSetup -> TangemPayCardLimitSetupComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayDetailsContainerComponent.Params(
                userWalletId = params.userWalletId,
                config = params.config,
            ),
        )
        TangemPayDetailsInnerRoute.LimitSetupSuccess -> TangemPayCardLimitSetupSuccessComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
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
    interface Factory : TangemPayCardPageComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayCardPageComponent.Params,
        ): DefaultTangemPayCardPageComponent
    }
}