package com.tangem.features.tangempay.cashback.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.tangempay.cashback.api.TangemPayCashbackComponent
import com.tangem.features.tangempay.cashback.impl.model.TangemPayCashbackModel
import com.tangem.features.tangempay.cashback.impl.ui.TangemPayCashbackScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayCashbackComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: TangemPayCashbackComponent.Params,
) : TangemPayCashbackComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayCashbackModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        TangemPayCashbackScreen(state = state, modifier = modifier)
    }

    @AssistedFactory
    interface Factory : TangemPayCashbackComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayCashbackComponent.Params,
        ): DefaultTangemPayCashbackComponent
    }
}