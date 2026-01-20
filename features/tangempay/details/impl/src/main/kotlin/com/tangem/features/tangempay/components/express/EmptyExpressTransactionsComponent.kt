package com.tangem.features.tangempay.components.express

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionsBlockState
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.tokendetails.ExpressTransactionsComponent
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Stable
internal class EmptyExpressTransactionsComponent(
    context: AppComponentContext,
) : AppComponentContext by context, ExpressTransactionsComponent {

    override val state: StateFlow<ExpressTransactionsBlockState> = MutableStateFlow(getInitialState())

    override fun LazyListScope.expressTransactionsContent(
        state: PersistentList<ExpressTransactionStateUM>,
        modifier: Modifier,
    ) {}

    private fun getInitialState(): ExpressTransactionsBlockState {
        return ExpressTransactionsBlockState(
            transactions = persistentListOf(),
            bottomSheetSlot = null,
            dialogSlot = null,
        )
    }
}