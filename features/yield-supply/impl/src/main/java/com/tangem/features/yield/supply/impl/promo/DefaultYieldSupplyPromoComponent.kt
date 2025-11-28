package com.tangem.features.yield.supply.impl.promo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.yield.supply.api.YieldSupplyPromoComponent
import com.tangem.features.yield.supply.impl.apy.YieldSupplyApyComponent
import com.tangem.features.yield.supply.impl.promo.model.YieldSupplyPromoModel
import com.tangem.features.yield.supply.impl.promo.ui.YieldSupplyPromoContent
import com.tangem.features.yield.supply.impl.subcomponents.startearning.YieldSupplyStartEarningEntryComponent
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
        childFactory = { config, componentContext -> bottomSheetChild(config, componentContext) },
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

    private fun bottomSheetChild(
        config: YieldSupplyPromoConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (config) {
        YieldSupplyPromoConfig.Apy -> YieldSupplyApyComponent(
            appComponentContext = childByContext(componentContext),
            params = YieldSupplyApyComponent.Params(
                cryptoCurrency = params.currency,
                apy = params.apy,
                onBackClick = {
                    model.bottomSheetNavigation.dismiss()
                },
            ),
        )
        YieldSupplyPromoConfig.Action -> YieldSupplyStartEarningEntryComponent(
            appComponentContext = childByContext(componentContext),
            params = YieldSupplyStartEarningEntryComponent.Params(
                userWalletId = params.userWalletId,
                cryptoCurrency = params.currency,
                onDismiss = { isProcessing ->
                    if (isProcessing) {
                        router.pop()
                    } else {
                        model.bottomSheetNavigation.dismiss()
                    }
                },
            ),
        )
    }

    @AssistedFactory
    interface Factory : YieldSupplyPromoComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: YieldSupplyPromoComponent.Params,
        ): DefaultYieldSupplyPromoComponent
    }
}