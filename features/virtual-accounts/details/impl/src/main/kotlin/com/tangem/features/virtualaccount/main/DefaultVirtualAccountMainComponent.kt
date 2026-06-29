package com.tangem.features.virtualaccount.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.virtualaccount.details.component.VirtualAccountMainComponent
import com.tangem.features.virtualaccount.main.addfunds.VirtualAccountAddFundsBottomSheetComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultVirtualAccountMainComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: VirtualAccountMainComponent.Params,
) : VirtualAccountMainComponent, AppComponentContext by appComponentContext {

    private val model: VirtualAccountMainModel = getOrCreateModel(params = params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = VirtualAccountMainNavigationBottomSheetConfig.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        VirtualAccountMainScreen(state = state, modifier = modifier)
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: VirtualAccountMainNavigationBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        return when (config) {
            is VirtualAccountMainNavigationBottomSheetConfig.AddFunds -> VirtualAccountAddFundsBottomSheetComponent(
                appComponentContext = childByContext(componentContext),
                params = VirtualAccountAddFundsBottomSheetComponent.Params(
                    userWalletId = params.userWalletId,
                    listener = model,
                    requisites = config.requisites,
                    dailyDepositLimit = config.dailyDepositLimit,
                ),
            )
        }
    }

    @AssistedFactory
    interface Factory : VirtualAccountMainComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: VirtualAccountMainComponent.Params,
        ): DefaultVirtualAccountMainComponent
    }
}