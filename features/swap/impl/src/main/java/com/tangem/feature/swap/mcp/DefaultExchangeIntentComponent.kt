package com.tangem.feature.swap.mcp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.feature.swap.mcp.model.ExchangeIntentModel
import com.tangem.feature.swap.mcp.ui.ExchangeIntentModal
import com.tangem.features.swap.mcp.ExchangeIntentComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultExchangeIntentComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: ExchangeIntentComponent.Params,
) : ExchangeIntentComponent, AppComponentContext by appComponentContext {

    private val model: ExchangeIntentModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        ExchangeIntentModal(state = state, onDismiss = model::onClose)
    }

    @AssistedFactory
    interface Factory : ExchangeIntentComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: ExchangeIntentComponent.Params,
        ): DefaultExchangeIntentComponent
    }
}