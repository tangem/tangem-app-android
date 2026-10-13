package com.tangem.features.moneyaccount.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.moneyaccount.details.component.MoneyAccountMainComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultMoneyAccountMainComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: MoneyAccountMainComponent.Params,
) : MoneyAccountMainComponent, AppComponentContext by appComponentContext {

    private val model: MoneyAccountMainModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        MoneyAccountMainScreen(state = state, modifier = modifier)
    }

    @AssistedFactory
    interface Factory : MoneyAccountMainComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: MoneyAccountMainComponent.Params,
        ): DefaultMoneyAccountMainComponent
    }
}