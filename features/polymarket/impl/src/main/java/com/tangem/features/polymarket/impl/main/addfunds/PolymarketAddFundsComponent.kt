package com.tangem.features.polymarket.impl.main.addfunds

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.polymarket.impl.main.addfunds.ui.PolymarketAddFundsBottomSheet

internal class PolymarketAddFundsComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: PolymarketAddFundsModel = getOrCreateModel(params = params)

    override fun dismiss() {
        model.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        PolymarketAddFundsBottomSheet(state = model.uiState)
    }

    data class Params(
        val callback: ModelCallback,
    )

    interface ModelCallback {
        fun onSwapClick()
        fun onReceiveClick()
        fun onDismissAddFunds()
    }
}