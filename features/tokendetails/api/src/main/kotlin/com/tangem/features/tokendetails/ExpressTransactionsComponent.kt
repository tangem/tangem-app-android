package com.tangem.features.tokendetails

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionsBlockState
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.flow.StateFlow

@Stable
interface ExpressTransactionsComponent {

    val state: StateFlow<ExpressTransactionsBlockState>

    fun LazyListScope.expressTransactionsContent(state: PersistentList<ExpressTransactionStateUM>, modifier: Modifier)

    data class Params(
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
    )

    interface Factory : ComponentFactory<Params, ExpressTransactionsComponent>
}