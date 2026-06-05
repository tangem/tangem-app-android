package com.tangem.features.tangempay.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
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
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.account.AccountStatus
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.limit.setup.TangemPayCardLimitSetupComponent
import com.tangem.features.tangempay.limit.setup.TangemPayCardLimitSetupSuccessComponent
import com.tangem.features.tangempay.navigation.TangemPayCardDetailsInnerRoute
import com.tangem.features.tokenreceive.TokenReceiveComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class TangemPayCardPageComponent @AssistedInject constructor(
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: Params,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
    private val tangemPayFeatureToggles: TangemPayFeatureToggles,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<TangemPayCardDetailsInnerRoute>()

    private val innerRouter = InnerRouter<TangemPayCardDetailsInnerRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val childStack = childStack(
        key = "tangemPayCardPageInnerStack",
        source = stackNavigation,
        serializer = TangemPayCardDetailsInnerRoute.serializer(),
        initialConfiguration = TangemPayCardDetailsInnerRoute.Details,
        childFactory = ::screenChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val childStack by childStack.subscribeAsState()
        BackHandler(onBack = ::onChildBack)
        Children(
            modifier = modifier,
            stack = childStack,
        ) { child ->
            child.instance.Content(modifier = Modifier.fillMaxSize())
        }
    }

    private fun screenChild(
        config: TangemPayCardDetailsInnerRoute,
        componentContext: ComponentContext,
    ): ComposableContentComponent = when (config) {
        TangemPayCardDetailsInnerRoute.Details -> TangemPayCardPageScreenComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = params,
            tokenReceiveComponentFactory = tokenReceiveComponentFactory,
        )
        TangemPayCardDetailsInnerRoute.ChangePIN -> TangemPayChangePinComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayDetailsContainerComponent.Params(initialStatus = params.initialStatus),
        )
        TangemPayCardDetailsInnerRoute.ChangePINSuccess -> TangemPayChangePinSuccessComponent(
            appComponentContext = childByContext(
                componentContext = componentContext,
                router = innerRouter,
            ),
            isRedesignEnabled = tangemPayFeatureToggles.isRedesignEnabled,
        )
        TangemPayCardDetailsInnerRoute.AddToWallet -> TangemPayAddToWalletComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayDetailsContainerComponent.Params(initialStatus = params.initialStatus),
        )
        TangemPayCardDetailsInnerRoute.EditCardDisplayName -> TangemPayEditDisplayNameComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayDetailsContainerComponent.Params(initialStatus = params.initialStatus),
        )
        TangemPayCardDetailsInnerRoute.LimitSetup -> TangemPayCardLimitSetupComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayDetailsContainerComponent.Params(initialStatus = params.initialStatus),
        )
        TangemPayCardDetailsInnerRoute.LimitSetupSuccess -> TangemPayCardLimitSetupSuccessComponent(
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

    data class Params(val initialStatus: AccountStatus.Payment)

    @AssistedFactory
    interface Factory : ComponentFactory<Params, TangemPayCardPageComponent> {
        override fun create(context: AppComponentContext, params: Params): TangemPayCardPageComponent
    }
}