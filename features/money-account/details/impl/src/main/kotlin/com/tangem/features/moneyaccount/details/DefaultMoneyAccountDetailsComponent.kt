package com.tangem.features.moneyaccount.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.moneyaccount.details.component.MoneyAccountDetailsComponent
import com.tangem.features.txhistory.component.TxHistoryComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultMoneyAccountDetailsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: MoneyAccountDetailsComponent.Params,
    txHistoryComponentFactory: TxHistoryComponent.Factory,
) : MoneyAccountDetailsComponent, AppComponentContext by appComponentContext {

    private val model: MoneyAccountDetailsModel = getOrCreateModel(params = params)

    private val txHistoryComponent = txHistoryComponentFactory.create(
        context = child("txHistoryComponent"),
        params = TxHistoryComponent.Params(
            userWalletId = params.userWalletId,
            currency = model.currency,
            openExplorer = model::onExploreClick,
        ),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        MoneyAccountDetailsScreen(
            state = state,
            txHistoryComponent = txHistoryComponent,
            modifier = modifier,
        )
        txHistoryComponent.Content(modifier)
    }

    @AssistedFactory
    interface Factory : MoneyAccountDetailsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: MoneyAccountDetailsComponent.Params,
        ): DefaultMoneyAccountDetailsComponent
    }
}