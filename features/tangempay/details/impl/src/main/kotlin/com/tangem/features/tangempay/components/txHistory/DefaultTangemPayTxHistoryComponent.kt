package com.tangem.features.tangempay.components.txHistory

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.tangempay.entity.TangemPayTxHistoryUM
import com.tangem.features.tangempay.model.TangemPayTxHistoryModel
import com.tangem.features.tangempay.ui.tangemPayTxHistoryItems
import kotlinx.coroutines.flow.StateFlow

internal class DefaultTangemPayTxHistoryComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : AppComponentContext by appComponentContext, TangemPayTxHistoryComponent {

    private val model: TangemPayTxHistoryModel = getOrCreateModel(params = params)
    override val state: StateFlow<TangemPayTxHistoryUM> = model.uiState

    override fun LazyListScope.txHistoryContent(listState: LazyListState, state: TangemPayTxHistoryUM) {
        tangemPayTxHistoryItems(listState, state)
    }

    data class Params(val customerWalletAddress: String)
}