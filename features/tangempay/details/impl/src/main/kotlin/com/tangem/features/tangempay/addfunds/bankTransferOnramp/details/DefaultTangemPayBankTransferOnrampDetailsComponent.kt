package com.tangem.features.tangempay.addfunds.bankTransferOnramp.details

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.tangempay.components.TangemPayBankTransferOnrampDetailsComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayBankTransferOnrampDetailsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: TangemPayBankTransferOnrampDetailsComponent.Params,
) : TangemPayBankTransferOnrampDetailsComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayBankTransferOnrampDetailsModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler(onBack = model::onCloseClick)
        val state by model.uiState.collectAsStateWithLifecycle()
        TangemPayBankTransferOnrampDetailsScreen(state = state, modifier = modifier)
    }

    @AssistedFactory
    interface Factory : TangemPayBankTransferOnrampDetailsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayBankTransferOnrampDetailsComponent.Params,
        ): DefaultTangemPayBankTransferOnrampDetailsComponent
    }
}