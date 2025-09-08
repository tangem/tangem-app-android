package com.tangem.features.yieldlending.impl.promo

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
import com.tangem.features.yieldlending.api.YieldLendingPromoComponent
import com.tangem.features.yieldlending.impl.promo.entity.StartEarningBottomSheetConfig
import com.tangem.features.yieldlending.impl.promo.model.YieldLendingPromoModel
import com.tangem.features.yieldlending.impl.promo.ui.YieldLendingPromoContent
import com.tangem.features.yieldlending.impl.subcomponents.startearning.YieldLendingActionComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultYieldLendingPromoComponent @AssistedInject constructor(
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: YieldLendingPromoComponent.Params,
) : YieldLendingPromoComponent, AppComponentContext by appComponentContext {

    private val model: YieldLendingPromoModel = getOrCreateModel(params = params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        YieldLendingPromoContent(
            onClick = model::onClick,
            modifier = modifier
        )

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: StartEarningBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = YieldLendingActionComponent(
        appComponentContext = childByContext(componentContext),
        params = YieldLendingActionComponent.Params(
            userWalletId = config.userWalletId,
            cryptoCurrency = config.cryptoCurrency,
            onDismiss = model.bottomSheetNavigation::dismiss,
        ),
    )

    @AssistedFactory
    interface Factory : YieldLendingPromoComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: YieldLendingPromoComponent.Params,
        ): DefaultYieldLendingPromoComponent
    }
}