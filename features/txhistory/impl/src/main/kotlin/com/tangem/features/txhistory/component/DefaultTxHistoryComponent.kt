package com.tangem.features.txhistory.component

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.txhistory.entity.TxHistoryUM
import com.tangem.features.txhistory.model.TxHistoryModel
import com.tangem.features.txhistory.ui.txHistoryItems
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow

internal class DefaultTxHistoryComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: TxHistoryComponent.Params,
) : TxHistoryComponent, AppComponentContext by appComponentContext {

    private val model: TxHistoryModel = getOrCreateModel(params)

    override val txHistoryState: StateFlow<TxHistoryUM>
        get() = model.uiState

    override fun LazyListScope.txHistoryContent(listState: LazyListState, state: TxHistoryUM) {
        txHistoryItems(listState, state)
    }

    @AssistedFactory
    interface Factory : TxHistoryComponent.Factory {
        override fun create(context: AppComponentContext, params: TxHistoryComponent.Params): DefaultTxHistoryComponent
    }
}