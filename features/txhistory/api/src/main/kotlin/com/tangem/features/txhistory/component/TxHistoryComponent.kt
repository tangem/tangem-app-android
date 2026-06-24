package com.tangem.features.txhistory.component

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.features.txhistory.entity.TxHistoryItemsUM
import com.tangem.features.txhistory.entity.TxHistoryUM
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Stable
interface TxHistoryComponent {

    val legacyTxHistoryState: StateFlow<TxHistoryUM>

    val txHistoryState: StateFlow<TxHistoryItemsUM>

    fun LazyListScope.txHistoryContentLegacy(listState: LazyListState, state: TxHistoryUM)

    fun LazyListScope.txHistoryContent(listState: LazyListState, state: TxHistoryItemsUM)

    data class Params(
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
        val openExplorer: () -> Unit,
        val onTxDetailsRequested: (Flow<TxHistoryInfo>) -> Unit,
    )

    interface Factory : ComponentFactory<Params, TxHistoryComponent>
}