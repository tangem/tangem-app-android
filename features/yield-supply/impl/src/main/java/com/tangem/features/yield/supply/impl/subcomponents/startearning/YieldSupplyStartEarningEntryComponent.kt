package com.tangem.features.yield.supply.impl.subcomponents.startearning

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.yield.supply.impl.subcomponents.feepolicy.YieldSupplyFeePolicyComponent
import com.tangem.features.yield.supply.impl.subcomponents.startearning.model.YieldSupplyStartEarningEntryModel
import com.tangem.features.yield.supply.impl.subcomponents.startearning.ui.YieldSupplyStartEarningBottomSheet

internal class YieldSupplyStartEarningEntryComponent(
    private val appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<YieldSupplyStartEarningRoute>()

    private val innerRouter = InnerRouter<YieldSupplyStartEarningRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val model: YieldSupplyStartEarningEntryModel = getOrCreateModel(params = params, router = innerRouter)

    private val innerStack = childStack(
        key = "startEarningStack",
        source = stackNavigation,
        serializer = null,
        initialConfiguration = YieldSupplyStartEarningRoute.Action,
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
        params.onDismiss(false)
    }

    @Composable
    override fun BottomSheet() {
        val stackState by innerStack.subscribeAsState()
        val uiState by model.uiState.collectAsStateWithLifecycle()

        YieldSupplyStartEarningBottomSheet(
            stackState = stackState,
            dismissOnClickOutside = { !uiState.isTransactionSending },
            onDismiss = ::dismiss,
        )
    }

    private fun createChild(
        route: YieldSupplyStartEarningRoute,
        factoryContext: AppComponentContext,
    ): ComposableModularContentComponent = when (route) {
        YieldSupplyStartEarningRoute.FeePolicy -> YieldSupplyFeePolicyComponent(
            appComponentContext = factoryContext,
            params = YieldSupplyFeePolicyComponent.Params(
                userWalletId = params.userWalletId,
                cryptoCurrency = params.cryptoCurrency,
                yieldSupplyActionUMFlow = model.uiState,
                callback = model,
            ),
        )
        YieldSupplyStartEarningRoute.Action -> YieldSupplyStartEarningComponent(
            appComponentContext = factoryContext,
            params = YieldSupplyStartEarningComponent.Params(
                userWalletId = params.userWalletId,
                cryptoCurrency = params.cryptoCurrency,
                yieldSupplyActionUM = model.uiState.value,
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
        val userWalletId: UserWalletId,
        val cryptoCurrency: CryptoCurrency,
        val onDismiss: (Boolean) -> Unit,
    )
}