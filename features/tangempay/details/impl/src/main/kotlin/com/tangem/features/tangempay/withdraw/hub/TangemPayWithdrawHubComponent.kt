package com.tangem.features.tangempay.withdraw.hub

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.tangempay.eligibility.TangemPayCountrySelectorComponent
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.TangemPayBankWithdrawComponent
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawOptions
import com.tangem.features.tangempay.withdraw.hub.model.TangemPayWithdrawHubModel
import com.tangem.features.tangempay.withdraw.hub.navigation.TangemPayWithdrawHubRoute
import com.tangem.features.tangempay.withdraw.hub.ui.TangemPayWithdrawHubScreen
import kotlinx.coroutines.flow.StateFlow

internal class TangemPayWithdrawHubComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
    private val countrySelectorFactory: TangemPayCountrySelectorComponent.Factory,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayWithdrawHubModel = getOrCreateModel(
        params = TangemPayWithdrawHubModel.Params(
            userWalletId = params.userWalletId,
            onOpenBankWithdraw = ::openBankWithdraw,
        ),
    )

    private val stackNavigation = StackNavigation<TangemPayWithdrawHubRoute>()

    private val innerRouter = InnerRouter<TangemPayWithdrawHubRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val hubScreenComponent = ComposableContentComponent { modifier ->
        val state by model.uiState.collectAsStateWithLifecycle()
        TangemPayWithdrawHubScreen(state = state, modifier = modifier)
    }

    private val childStack = childStack(
        key = "tangemPayWithdrawHubStack",
        source = stackNavigation,
        serializer = null,
        initialConfiguration = TangemPayWithdrawHubRoute.Hub,
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
        config: TangemPayWithdrawHubRoute,
        componentContext: ComponentContext,
    ): ComposableContentComponent = when (config) {
        TangemPayWithdrawHubRoute.Hub -> hubScreenComponent
        is TangemPayWithdrawHubRoute.BankWithdraw -> TangemPayBankWithdrawComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayBankWithdrawComponent.Params(
                userWalletId = params.userWalletId,
                entry = config.entry,
                options = config.options,
            ),
            countrySelectorFactory = countrySelectorFactory,
        )
    }

    private fun openBankWithdraw(entry: BankWithdrawEntry, options: StateFlow<BankWithdrawOptions>) {
        stackNavigation.pushNew(TangemPayWithdrawHubRoute.BankWithdraw(entry = entry, options = options))
    }

    private fun onChildBack() {
        if (childStack.value.backStack.isEmpty()) {
            router.pop()
        } else {
            stackNavigation.pop()
        }
    }

    data class Params(val userWalletId: UserWalletId)
}