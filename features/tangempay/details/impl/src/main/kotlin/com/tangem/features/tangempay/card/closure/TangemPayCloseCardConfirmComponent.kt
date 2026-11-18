package com.tangem.features.tangempay.card.closure

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.message.modal.MessageModal

internal class TangemPayCloseCardConfirmComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayCloseCardConfirmModel = getOrCreateModel(params = params)

    override fun dismiss() = model.onDismiss()

    @Composable
    override fun BottomSheet() {
        val state by model.state.collectAsStateWithLifecycle()
        MessageModal(state = state, onDismissRequest = model::onDismiss)
    }

    data class Params(
        val listener: CloseCardConfirmListener,
        val card: TangemPayClosingCard,
        val canFreeze: Boolean,
    )
}

internal interface CloseCardConfirmListener {
    fun onCloseCardConfirmDismissed()

    fun onFreezeInsteadOfClose()
}