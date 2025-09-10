package com.tangem.features.tangempay.components.txHistory

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.tangempay.model.TangemPayTxHistoryModel
import com.tangem.features.txhistory.entity.TxHistoryUM
import com.tangem.features.txhistory.ui.txHistoryItems
import kotlinx.coroutines.flow.StateFlow

internal class DefaultTangemPayTxHistoryComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : AppComponentContext by appComponentContext, TangemPayTxHistoryComponent {

    private val model: TangemPayTxHistoryModel = getOrCreateModel(params = params)
    override val state: StateFlow<TxHistoryUM> = model.uiState

    override fun LazyListScope.txHistoryContent(listState: LazyListState, state: TxHistoryUM) {
        txHistoryItems(listState, state)
    }

    data class Params(val userWalletId: UserWalletId)
}