package com.tangem.features.moneyaccount.main.addfunds

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.moneyaccount.details.component.MoneyAccountAddFundsBottomSheetComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultMoneyAccountAddFundsBottomSheetComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: MoneyAccountAddFundsBottomSheetComponent.Params,
) : MoneyAccountAddFundsBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: MoneyAccountAddFundsModel = getOrCreateModel(params = params)

    override fun dismiss() {
        model.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        MoneyAccountAddFundsBottomSheet(state = state)
    }

    @AssistedFactory
    interface Factory : MoneyAccountAddFundsBottomSheetComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: MoneyAccountAddFundsBottomSheetComponent.Params,
        ): DefaultMoneyAccountAddFundsBottomSheetComponent
    }
}