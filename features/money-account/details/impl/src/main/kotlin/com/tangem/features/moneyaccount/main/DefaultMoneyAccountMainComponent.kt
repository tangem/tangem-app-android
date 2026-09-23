package com.tangem.features.moneyaccount.main

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
import com.tangem.features.moneyaccount.details.component.MoneyAccountAddFundsBottomSheetComponent
import com.tangem.features.moneyaccount.details.component.MoneyAccountMainComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultMoneyAccountMainComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: MoneyAccountMainComponent.Params,
    private val addFundsComponentFactory: MoneyAccountAddFundsBottomSheetComponent.Factory,
) : MoneyAccountMainComponent, AppComponentContext by appComponentContext {

    private val model: MoneyAccountMainModel = getOrCreateModel(params = params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = MoneyAccountMainNavigationBottomSheetConfig.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        MoneyAccountMainScreen(state = state, modifier = modifier)
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: MoneyAccountMainNavigationBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        return when (config) {
            is MoneyAccountMainNavigationBottomSheetConfig.AddFunds -> addFundsComponentFactory.create(
                context = childByContext(componentContext),
                params = MoneyAccountAddFundsBottomSheetComponent.Params(
                    userWalletId = params.userWalletId,
                    listener = model,
                    requisites = config.requisites,
                    dailyDepositLimit = config.dailyDepositLimit,
                ),
            )
        }
    }

    @AssistedFactory
    interface Factory : MoneyAccountMainComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: MoneyAccountMainComponent.Params,
        ): DefaultMoneyAccountMainComponent
    }
}