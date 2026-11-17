package com.tangem.features.moneyaccount.main.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.moneyaccount.main.model.MoneyAccountActivationModel
import com.tangem.features.moneyaccount.main.ui.MoneyAccountActivationBottomSheet
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultMoneyAccountActivationComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: MoneyAccountActivationComponent.Params,
) : MoneyAccountActivationComponent, AppComponentContext by context {

    private val model: MoneyAccountActivationModel = getOrCreateModel(params = params)

    override fun dismiss() {
        model.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        MoneyAccountActivationBottomSheet(state = state)
    }

    @AssistedFactory
    interface Factory : MoneyAccountActivationComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: MoneyAccountActivationComponent.Params,
        ): DefaultMoneyAccountActivationComponent
    }
}