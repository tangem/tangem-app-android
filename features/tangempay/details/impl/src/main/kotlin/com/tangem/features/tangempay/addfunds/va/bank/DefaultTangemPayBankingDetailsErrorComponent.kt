package com.tangem.features.tangempay.addfunds.va.bank

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.tangempay.components.TangemPayBankingDetailsErrorComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayBankingDetailsErrorComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: TangemPayBankingDetailsErrorComponent.Params,
) : TangemPayBankingDetailsErrorComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayBankingDetailsErrorModel = getOrCreateModel(params = params)

    override fun dismiss() {
        model.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        TangemPayBankingDetailsErrorBottomSheet(state = state)
    }

    @AssistedFactory
    interface Factory : TangemPayBankingDetailsErrorComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayBankingDetailsErrorComponent.Params,
        ): DefaultTangemPayBankingDetailsErrorComponent
    }
}