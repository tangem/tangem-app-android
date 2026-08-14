package com.tangem.features.tangempay.account

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
import com.tangem.domain.models.pay.TangemPayDetailsInitialRoute
import com.tangem.features.promobanners.api.PromoBannersBlockComponent
import com.tangem.features.tangempay.addfunds.va.deposit.TangemPayVirtualAccountDepositSuccessComponent
import com.tangem.features.tangempay.card.details.TangemPayCardPageComponent
import com.tangem.features.tangempay.card.gpay.TangemPayAddToWalletComponent
import com.tangem.features.tangempay.cashback.api.TangemPayCashbackComponent
import com.tangem.features.tangempay.common.tariffPlan
import com.tangem.features.tangempay.common.userWalletId
import com.tangem.features.tangempay.components.TangemPayDetailsContainerComponent
import com.tangem.features.tangempay.orderCard.api.TangemPayOrderCardComponent
import com.tangem.features.tangempay.tiers.current.TangemPayCurrentPlanComponent
import com.tangem.features.tangempay.tiers.select.TangemPaySelectPlanComponent
import com.tangem.features.tangempay.tiers.select.TangemPaySelectPlanSource
import com.tangem.features.tokendetails.ExpressTransactionsComponent
import com.tangem.features.tokenreceive.TokenReceiveComponent
import com.tangem.features.virtualaccount.details.component.VirtualAccountAddFundsBottomSheetComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("LongParameterList")
internal class DefaultTangemPayDetailsContainerComponent @AssistedInject constructor(
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: TangemPayDetailsContainerComponent.Params,
    private val tangemPayCardPageFactory: TangemPayCardPageComponent.Factory,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
    private val expressTransactionsComponentFactory: ExpressTransactionsComponent.Factory,
    private val promoBannersBlockComponentFactory: PromoBannersBlockComponent.Factory,
    private val virtualAccountAddFundsComponentFactory: VirtualAccountAddFundsBottomSheetComponent.Factory,
    private val cashbackComponentFactory: TangemPayCashbackComponent.Factory,
    private val orderCardComponentFactory: TangemPayOrderCardComponent.Factory,
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
        initialConfiguration = resolveInitialConfiguration(),
        childFactory = ::screenChild,
    )

    private fun resolveInitialConfiguration(): TangemPayAccountDetailsInnerRoute {
        val tariffPlan = params.initialStatus.tariffPlan
        return when (params.initialRoute) {
            TangemPayDetailsInitialRoute.ACCOUNT_DETAILS,
            TangemPayDetailsInitialRoute.ADD_FUNDS,
            -> TangemPayAccountDetailsInnerRoute.AccountDetails
            TangemPayDetailsInitialRoute.TIERS_ONBOARDING -> if (tariffPlan != null) {
                TangemPayAccountDetailsInnerRoute.SelectPlan(
                    tariffPlan = tariffPlan,
                    source = TangemPaySelectPlanSource.TIERS_ONBOARDING,
                )
            } else {
                TangemPayAccountDetailsInnerRoute.AccountDetails
            }
        }
    }

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
            virtualAccountAddFundsComponentFactory = virtualAccountAddFundsComponentFactory,
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
                userWalletId = params.initialStatus.userWalletId,
                tariffPlan = config.tariffPlan,
            ),
        )
        is TangemPayAccountDetailsInnerRoute.SelectPlan -> TangemPaySelectPlanComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPaySelectPlanComponent.Params(
                userWalletId = params.initialStatus.userWalletId,
                tariffPlan = config.tariffPlan,
                source = config.source,
            ),
        )
        TangemPayAccountDetailsInnerRoute.VirtualAccountDepositSuccess ->
            TangemPayVirtualAccountDepositSuccessComponent(
                appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            )
        TangemPayAccountDetailsInnerRoute.Cashback -> cashbackComponentFactory.create(
            context = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayCashbackComponent.Params(
                userWalletId = params.initialStatus.userWalletId,
            ),
        )
        TangemPayAccountDetailsInnerRoute.OrderCard -> orderCardComponentFactory.create(
            context = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayOrderCardComponent.Params(
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