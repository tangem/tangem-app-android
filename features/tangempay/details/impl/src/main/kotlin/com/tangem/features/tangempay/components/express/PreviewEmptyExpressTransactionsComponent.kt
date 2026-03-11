package com.tangem.features.tangempay.components.express

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionsBlockState
import com.tangem.features.tokendetails.ExpressTransactionsComponent
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Cannot really preview anything here since the UM implementation [ExchangeUM] is in token:details module
 * For the actual preview @see [TokenDetailsScreen]
 **/
internal class PreviewEmptyExpressTransactionsComponent : ExpressTransactionsComponent {

    override val state: StateFlow<ExpressTransactionsBlockState> = MutableStateFlow(getInitialState())

    override fun LazyListScope.expressTransactionsContent(
        state: PersistentList<ExpressTransactionStateUM>,
        modifier: Modifier,
    ) {}

    private fun getInitialState(): ExpressTransactionsBlockState {
        return ExpressTransactionsBlockState(
            transactions = persistentListOf(),
            transactionsToDisplay = persistentListOf(),
            bottomSheetSlot = null,
            dialogSlot = null,
        )
    }
}