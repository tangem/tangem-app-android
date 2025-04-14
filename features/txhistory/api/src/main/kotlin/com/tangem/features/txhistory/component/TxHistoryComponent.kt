package com.tangem.features.txhistory.component

import androidx.compose.runtime.Stable
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.txhistory.entity.TxHistoryUM
import kotlinx.coroutines.flow.StateFlow

@Stable
interface TxHistoryComponent {

    val txHistoryState: StateFlow<TxHistoryUM>

    fun LazyListScope.txHistoryContent(listState: LazyListState, state: TxHistoryUM)

    data class Params(
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
        val openExplorer: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, TxHistoryComponent>
}