package com.tangem.features.txhistory.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.txhistory.model.TxHistoryDetailsModel
import com.tangem.features.txhistory.ui.TxHistoryDetailsModalBottomSheetContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTxHistoryDetailsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: TxHistoryDetailsComponent.Params,
) : TxHistoryDetailsComponent, AppComponentContext by context {

    private val model: TxHistoryDetailsModel = getOrCreateModel(params)

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()

        TxHistoryDetailsModalBottomSheetContent(state = state, onDismiss = ::dismiss)
    }

    @AssistedFactory
    interface Factory : TxHistoryDetailsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TxHistoryDetailsComponent.Params,
        ): DefaultTxHistoryDetailsComponent
    }
}