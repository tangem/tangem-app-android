package com.tangem.features.tangempay.components.txHistory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.tangempay.components.TangemPayTransactionBottomSheetComponent
import com.tangem.features.tangempay.model.TangemPayTxHistoryDetailsModel
import com.tangem.features.tangempay.ui.TangemPayTxHistoryDetailsContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class TangemPayTxHistoryDetailsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: TangemPayTransactionBottomSheetComponent.Params,
) : TangemPayTransactionBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayTxHistoryDetailsModel = getOrCreateModel(params = params)

    override fun dismiss() {
        model.dismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        TangemPayTxHistoryDetailsContent(state = state)
    }

    @AssistedFactory
    interface Factory : TangemPayTransactionBottomSheetComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayTransactionBottomSheetComponent.Params,
        ): TangemPayTxHistoryDetailsComponent
    }
}