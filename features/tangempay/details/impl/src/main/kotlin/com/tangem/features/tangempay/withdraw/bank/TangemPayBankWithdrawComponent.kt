package com.tangem.features.tangempay.withdraw.bank

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
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawFlow
import com.tangem.features.tangempay.withdraw.bank.model.TangemPayBankWithdrawModel
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.features.tangempay.withdraw.bank.stub.TangemPayBankWithdrawStepStubComponent

internal class TangemPayBankWithdrawComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayBankWithdrawModel = getOrCreateModel(params = params)

    private val stackNavigation = StackNavigation<TangemPayBankWithdrawInnerRoute>()

    private val innerRouter = InnerRouter<TangemPayBankWithdrawInnerRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val childStack = childStack(
        key = "tangemPayBankWithdrawInnerStack",
        source = stackNavigation,
        serializer = TangemPayBankWithdrawInnerRoute.serializer(),
        initialConfiguration = BankWithdrawFlow.initialStep(params.entry),
        childFactory = ::screenChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val stack by childStack.subscribeAsState()
        BackHandler(onBack = ::onChildBack)
        Children(modifier = modifier, stack = stack) { child ->
            child.instance.Content(modifier = Modifier.fillMaxSize())
        }
    }

    private fun screenChild(
        config: TangemPayBankWithdrawInnerRoute,
        componentContext: ComponentContext,
    ): ComposableContentComponent = TangemPayBankWithdrawStepStubComponent(
        appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
        params = TangemPayBankWithdrawStepStubComponent.Params(draft = model.draft, step = config),
    )

    private fun onChildBack() {
        if (childStack.value.backStack.isEmpty()) {
            router.pop()
        } else {
            stackNavigation.pop()
        }
    }

    data class Params(
        val userWalletId: UserWalletId,
        val entry: BankWithdrawEntry,
    )
}