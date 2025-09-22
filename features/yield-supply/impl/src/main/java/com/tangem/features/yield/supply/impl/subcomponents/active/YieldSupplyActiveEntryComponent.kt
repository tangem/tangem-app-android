package com.tangem.features.yield.supply.impl.subcomponents.active

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.yield.supply.impl.subcomponents.active.model.YieldSupplyActiveEntryModel
import com.tangem.features.yield.supply.impl.subcomponents.active.model.YieldSupplyActiveRoute
import com.tangem.features.yield.supply.impl.subcomponents.active.ui.YieldSupplyActiveEntryBottomSheet
import com.tangem.features.yield.supply.impl.subcomponents.stopearning.YieldSupplyStopEarningComponent
import kotlinx.coroutines.flow.StateFlow

internal class YieldSupplyActiveEntryComponent(
    private val appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<YieldSupplyActiveRoute>()

    private val innerRouter = InnerRouter<YieldSupplyActiveRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val model: YieldSupplyActiveEntryModel = getOrCreateModel(params = params, router = innerRouter)

    private val innerStack = childStack(
        key = "yieldSupplyActiveStack",
        source = stackNavigation,
        serializer = null,
        initialConfiguration = YieldSupplyActiveRoute.Info,
        handleBackButton = true,
        childFactory = { configuration, factoryContext ->
            createChild(
                configuration,
                childByContext(
                    componentContext = factoryContext,
                    router = innerRouter,
                ),
            )
        },
    )

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val stackState by innerStack.subscribeAsState()

        YieldSupplyActiveEntryBottomSheet(
            stackState = stackState,
            onDismiss = ::dismiss,
        )
    }

    private fun createChild(
        route: YieldSupplyActiveRoute,
        factoryContext: AppComponentContext,
    ): ComposableModularContentComponent = when (route) {
        YieldSupplyActiveRoute.Info -> YieldSupplyActiveComponent(
            appComponentContext = factoryContext,
            params = YieldSupplyActiveComponent.Params(
                userWallet = params.userWallet,
                cryptoCurrencyStatusFlow = params.cryptoCurrencyStatusFlow,
                callback = model,
            ),
        )
        YieldSupplyActiveRoute.Action -> YieldSupplyStopEarningComponent(
            appComponentContext = factoryContext,
            params = YieldSupplyStopEarningComponent.Params(
                userWallet = params.userWallet,
                cryptoCurrencyStatusFlow = params.cryptoCurrencyStatusFlow,
                callback = model,
            ),
        )
    }

    private fun onChildBack() {
        if (innerStack.value.backStack.isEmpty()) {
            dismiss()
        } else {
            stackNavigation.pop()
        }
    }

    data class Params(
        val userWallet: UserWallet,
        val cryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>,
        val onDismiss: () -> Unit,
    )
}