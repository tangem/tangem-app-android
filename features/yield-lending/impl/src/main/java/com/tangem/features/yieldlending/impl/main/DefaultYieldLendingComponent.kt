package com.tangem.features.yieldlending.impl.main

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
import com.tangem.features.yieldlending.api.YieldLendingComponent
import com.tangem.features.yieldlending.impl.main.model.YieldLendingModel
import com.tangem.features.yieldlending.impl.main.ui.YieldLendingBlockContent
import com.tangem.features.yieldlending.impl.subcomponents.active.YieldLendingActiveComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultYieldLendingComponent @AssistedInject constructor(
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: YieldLendingComponent.Params,
) : YieldLendingComponent, AppComponentContext by appComponentContext {

    private val model: YieldLendingModel = getOrCreateModel(params = params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val yieldLendingUM by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        YieldLendingBlockContent(yieldLendingUM = yieldLendingUM, model::onClick, modifier = modifier)

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: Unit,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = YieldLendingActiveComponent(
        appComponentContext = childByContext(componentContext),
        params = YieldLendingActiveComponent.Params(
            userWallet = model.userWallet,
            cryptoCurrencyStatusFlow = model.cryptoCurrencyStatusFlow,
            onDismiss = model.bottomSheetNavigation::dismiss,
        ),
    )

    @AssistedFactory
    interface Factory : YieldLendingComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: YieldLendingComponent.Params,
        ): DefaultYieldLendingComponent
    }
}