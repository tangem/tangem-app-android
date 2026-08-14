package com.tangem.features.tangempay.multichain.othernetworks

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

internal class PaymentOtherNetworksComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: PaymentOtherNetworksModel = getOrCreateModel(params = params)

    override fun dismiss() {
        model.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        PaymentOtherNetworksContent(state = model.uiState)
    }

    data class Params(
        val onDismiss: () -> Unit,
    )
}