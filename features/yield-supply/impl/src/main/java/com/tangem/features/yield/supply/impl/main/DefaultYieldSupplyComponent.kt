package com.tangem.features.yield.supply.impl.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.yield.supply.api.YieldSupplyComponent
import com.tangem.features.yield.supply.impl.main.model.YieldSupplyModel
import com.tangem.features.yield.supply.impl.main.ui.YieldSupplyBlockContent
import com.tangem.features.yield.supply.impl.subcomponents.active.YieldSupplyActiveEntryComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultYieldSupplyComponent @AssistedInject constructor(
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: YieldSupplyComponent.Params,
) : YieldSupplyComponent, AppComponentContext by appComponentContext {

    private val model: YieldSupplyModel = getOrCreateModel(params = params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = { _, context -> bottomSheetChild(context) },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val yieldSupplyUM by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        YieldSupplyBlockContent(yieldSupplyUM = yieldSupplyUM, modifier = modifier)

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(componentContext: ComponentContext): ComposableBottomSheetComponent {
        return YieldSupplyActiveEntryComponent(
            appComponentContext = childByContext(componentContext),
            params = YieldSupplyActiveEntryComponent.Params(
                userWallet = model.userWallet,
                cryptoCurrencyStatusFlow = model.cryptoCurrencyStatusFlow,
                isBalanceHiddenFlow = model.isBalanceHiddenFlow,
                onDismiss = model.bottomSheetNavigation::dismiss,
            ),
        )
    }

    @AssistedFactory
    interface Factory : YieldSupplyComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: YieldSupplyComponent.Params,
        ): DefaultYieldSupplyComponent
    }
}