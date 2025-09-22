package com.tangem.features.yield.supply.impl.promo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.getEmptyComposableBottomSheetComponent
import com.tangem.features.yield.supply.api.YieldSupplyPromoComponent
import com.tangem.features.yield.supply.impl.promo.model.YieldSupplyPromoModel
import com.tangem.features.yield.supply.impl.promo.ui.YieldSupplyPromoContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultYieldSupplyPromoComponent @AssistedInject constructor(
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: YieldSupplyPromoComponent.Params,
) : YieldSupplyPromoComponent, AppComponentContext by appComponentContext {

    private val model: YieldSupplyPromoModel = getOrCreateModel(params = params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = { _, _ -> bottomSheetChild() },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state = model.uiState
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        YieldSupplyPromoContent(
            yieldSupplyPromoUM = state,
            clickIntents = model,
            modifier = modifier,
        )

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(): ComposableBottomSheetComponent = getEmptyComposableBottomSheetComponent()

    @AssistedFactory
    interface Factory : YieldSupplyPromoComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: YieldSupplyPromoComponent.Params,
        ): DefaultYieldSupplyPromoComponent
    }
}