package com.tangem.features.tangempay.cashback.impl

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
import com.tangem.features.tangempay.cashback.api.TangemPayCashbackComponent
import com.tangem.features.tangempay.cashback.impl.model.TangemPayCashbackModel
import com.tangem.features.tangempay.cashback.impl.model.TangemPayCashbackNavigation
import com.tangem.features.tangempay.cashback.impl.ui.TangemPayCashbackAccrualsBottomSheet
import com.tangem.features.tangempay.cashback.impl.ui.TangemPayCashbackDetailsBottomSheet
import com.tangem.features.tangempay.cashback.impl.ui.TangemPayCashbackScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayCashbackComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: TangemPayCashbackComponent.Params,
) : TangemPayCashbackComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayCashbackModel = getOrCreateModel(params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = TangemPayCashbackNavigation.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        TangemPayCashbackScreen(state = state, modifier = modifier)
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        navigation: TangemPayCashbackNavigation,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        val context = childByContext(componentContext)
        return when (navigation) {
            TangemPayCashbackNavigation.Details -> CashbackBottomSheetComponent(
                appComponentContext = context,
                onDismiss = model.bottomSheetNavigation::dismiss,
                content = {
                    val state by model.detailsSheet.collectAsStateWithLifecycle()
                    TangemPayCashbackDetailsBottomSheet(
                        state = state,
                        onDismiss = model.bottomSheetNavigation::dismiss,
                    )
                },
            )
            TangemPayCashbackNavigation.Accruals -> CashbackBottomSheetComponent(
                appComponentContext = context,
                onDismiss = model.bottomSheetNavigation::dismiss,
                content = {
                    val state by model.accrualsSheet.collectAsStateWithLifecycle()
                    TangemPayCashbackAccrualsBottomSheet(
                        state = state,
                        onDismiss = model.bottomSheetNavigation::dismiss,
                    )
                },
            )
        }
    }

    @AssistedFactory
    interface Factory : TangemPayCashbackComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayCashbackComponent.Params,
        ): DefaultTangemPayCashbackComponent
    }
}