package com.tangem.features.moneyaccount.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
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
import com.tangem.features.moneyaccount.details.component.MoneyAccountDetailsComponent
import com.tangem.features.tangempay.components.TangemPayBankTransferOnrampDetailsComponent
import com.tangem.features.tangempay.components.TangemPayBankingDetailsErrorComponent
import com.tangem.features.tokenreceive.TokenReceiveComponent
import com.tangem.features.txhistory.component.TxHistoryComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("LongParameterList")
internal class DefaultMoneyAccountDetailsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: MoneyAccountDetailsComponent.Params,
    private val txHistoryComponentFactory: TxHistoryComponent.Factory,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
    private val bankingDetailsErrorComponentFactory: TangemPayBankingDetailsErrorComponent.Factory,
    private val bankTransferOnrampDetailsComponentFactory: TangemPayBankTransferOnrampDetailsComponent.Factory,
    private val bankDetailsAnalytics: MoneyAccountBankDetailsAnalytics,
) : MoneyAccountDetailsComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<MoneyAccountDetailsRoute>()

    private val innerRouter = InnerRouter<MoneyAccountDetailsRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val childStack = childStack(
        key = "moneyAccountDetailsStack",
        source = stackNavigation,
        serializer = MoneyAccountDetailsRoute.serializer(),
        initialConfiguration = MoneyAccountDetailsRoute.Details,
        childFactory = ::screenChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val stack by childStack.subscribeAsState()

        BackHandler(onBack = router::pop)
        Children(stack = stack, modifier = modifier, animation = stackAnimation()) { child ->
            child.instance.Content(modifier = Modifier.fillMaxSize())
        }
    }

    private fun screenChild(
        config: MoneyAccountDetailsRoute,
        componentContext: ComponentContext,
    ): ComposableContentComponent = when (config) {
        MoneyAccountDetailsRoute.Details -> MoneyAccountDetailsScreenComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = params,
            txHistoryComponentFactory = txHistoryComponentFactory,
            tokenReceiveComponentFactory = tokenReceiveComponentFactory,
            bankingDetailsErrorComponentFactory = bankingDetailsErrorComponentFactory,
        )
        is MoneyAccountDetailsRoute.BankDetails -> bankTransferOnrampDetailsComponentFactory.create(
            context = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayBankTransferOnrampDetailsComponent.Params(
                userWalletId = params.userWalletId,
                bankCredentials = config.bankCredentials,
                analytics = bankDetailsAnalytics,
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
    interface Factory : MoneyAccountDetailsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: MoneyAccountDetailsComponent.Params,
        ): DefaultMoneyAccountDetailsComponent
    }
}