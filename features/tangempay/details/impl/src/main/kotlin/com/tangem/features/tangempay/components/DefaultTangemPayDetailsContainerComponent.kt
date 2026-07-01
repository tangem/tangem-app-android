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
import com.tangem.features.promobanners.api.PromoBannersBlockComponent
import com.tangem.features.tangempay.navigation.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.tiers.current.TangemPayCurrentPlanComponent
import com.tangem.features.tangempay.tiers.select.TangemPaySelectPlanComponent
import com.tangem.features.tangempay.utils.userWalletId
import com.tangem.features.tokendetails.ExpressTransactionsComponent
import com.tangem.features.tokenreceive.TokenReceiveComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayDetailsContainerComponent @AssistedInject constructor(
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: TangemPayDetailsContainerComponent.Params,
    private val tangemPayCardPageFactory: TangemPayCardPageComponent.Factory,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
    private val expressTransactionsComponentFactory: ExpressTransactionsComponent.Factory,
    private val promoBannersBlockComponentFactory: PromoBannersBlockComponent.Factory,
) : AppComponentContext by appComponentContext, TangemPayDetailsContainerComponent {

    private val stackNavigation = StackNavigation<TangemPayAccountDetailsInnerRoute>()

    private val innerRouter = InnerRouter<TangemPayAccountDetailsInnerRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val childStack = childStack(
        key = "tangemPayDetailsInnerStack",
        source = stackNavigation,
        serializer = TangemPayAccountDetailsInnerRoute.serializer(),
        initialConfiguration = TangemPayAccountDetailsInnerRoute.AccountDetails,
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
        config: TangemPayAccountDetailsInnerRoute,
        componentContext: ComponentContext,
    ): ComposableContentComponent = when (config) {
        TangemPayAccountDetailsInnerRoute.AccountDetails -> TangemPayDetailsComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = params,
            tokenReceiveComponentFactory = tokenReceiveComponentFactory,
            expressTransactionsComponentFactory = expressTransactionsComponentFactory,
            promoBannersBlockComponentFactory = promoBannersBlockComponentFactory,
        )
        is TangemPayAccountDetailsInnerRoute.CardDetails -> tangemPayCardPageFactory.create(
            context = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayCardPageComponent.Params(
                initialStatus = params.initialStatus,
                cardId = config.cardId,
            ),
        )
        is TangemPayAccountDetailsInnerRoute.AddToWallet -> TangemPayAddToWalletComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayAddToWalletComponent.Params(
                card = config.card,
                userWalletId = params.initialStatus.userWalletId,
            ),
        )
        is TangemPayAccountDetailsInnerRoute.CurrentPlan -> TangemPayCurrentPlanComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayCurrentPlanComponent.Params(
                tariffPlan = config.tariffPlan,
            ),
        )
        TangemPayAccountDetailsInnerRoute.SelectPlan -> TangemPaySelectPlanComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPaySelectPlanComponent.Params(
                userWalletId = params.initialStatus.userWalletId,
            ),
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