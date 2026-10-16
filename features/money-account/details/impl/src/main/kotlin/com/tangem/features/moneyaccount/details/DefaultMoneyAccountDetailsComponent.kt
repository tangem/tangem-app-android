package com.tangem.features.moneyaccount.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.moneyaccount.details.component.MoneyAccountDetailsComponent
import com.tangem.features.txhistory.component.TxHistoryComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultMoneyAccountDetailsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: MoneyAccountDetailsComponent.Params,
    txHistoryComponentFactory: TxHistoryComponent.Factory,
) : MoneyAccountDetailsComponent, AppComponentContext by appComponentContext {

    private val model: MoneyAccountDetailsModel = getOrCreateModel(params = params)

    private val txHistorySlot = childSlot(
        key = "txHistorySlot",
        source = model.txHistorySlotNavigation,
        serializer = null,
        childFactory = { currency, ctx ->
            txHistoryComponentFactory.create(
                context = childByContext(ctx),
                params = TxHistoryComponent.Params(
                    userWalletId = params.userWalletId,
                    currency = currency,
                    openExplorer = model::onExploreClick,
                ),
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val txHistorySlotState by txHistorySlot.subscribeAsState()
        val txHistoryComponent = txHistorySlotState.child?.instance
        MoneyAccountDetailsScreen(
            state = state,
            txHistoryComponent = txHistoryComponent,
            modifier = modifier,
        )
        txHistoryComponent?.Content(modifier)
    }

    @AssistedFactory
    interface Factory : MoneyAccountDetailsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: MoneyAccountDetailsComponent.Params,
        ): DefaultMoneyAccountDetailsComponent
    }
}