package com.tangem.feature.tokendetails.presentation

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tangem.common.ui.expressStatus.expressTransactionsItems
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionsBlockState
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.feature.tokendetails.presentation.tokendetails.model.ExpressTransactionsModel
import com.tangem.features.tokendetails.ExpressTransactionsComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.flow.StateFlow

@Stable
internal class DefaultExpressTransactionsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: ExpressTransactionsComponent.Params,
) : AppComponentContext by context, ExpressTransactionsComponent {

    private val model: ExpressTransactionsModel = getOrCreateModel(params = params)
    override val state: StateFlow<ExpressTransactionsBlockState> = model.uiState

    override fun LazyListScope.expressTransactionsContent(
        state: PersistentList<ExpressTransactionStateUM>,
        modifier: Modifier,
    ) {
        expressTransactionsItems(expressTxs = state, modifier = modifier)
    }

    @AssistedFactory
    interface Factory : ExpressTransactionsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: ExpressTransactionsComponent.Params,
        ): DefaultExpressTransactionsComponent
    }
}