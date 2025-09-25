package com.tangem.features.tangempay.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.NavigationBar3ButtonsScrim
import com.tangem.features.tangempay.components.txHistory.DefaultTangemPayTxHistoryComponent
import com.tangem.features.tangempay.model.TangemPayDetailsModel
import com.tangem.features.tangempay.ui.TangemPayDetailsScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayDetailsComponent @AssistedInject constructor(
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: TangemPayDetailsComponent.Params,
) : AppComponentContext by appComponentContext, TangemPayDetailsComponent {

    private val model: TangemPayDetailsModel = getOrCreateModel(params = params)
    private val txHistoryComponent = DefaultTangemPayTxHistoryComponent(
        appComponentContext = child("txHistoryComponent"),
        params = DefaultTangemPayTxHistoryComponent.Params(userWalletId = params.userWalletId),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        NavigationBar3ButtonsScrim()
        TangemPayDetailsScreen(state = state, txHistoryComponent = txHistoryComponent, modifier = modifier)
    }

    @AssistedFactory
    interface Factory : TangemPayDetailsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayDetailsComponent.Params,
        ): DefaultTangemPayDetailsComponent
    }
}