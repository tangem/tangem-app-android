package com.tangem.features.tangempay.eligibility.wallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.tangempay.eligibility.TangemPayWalletSelectorComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayWalletSelectorComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: TangemPayWalletSelectorComponent.Params,
) : TangemPayWalletSelectorComponent, AppComponentContext by context {

    private val model: TangemPayWalletSelectorModel = getOrCreateModel(params)

    override fun dismiss() {
        model.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        TangemPayWalletSelectorBottomSheet(state = state)
    }

    @AssistedFactory
    interface Factory : TangemPayWalletSelectorComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayWalletSelectorComponent.Params,
        ): DefaultTangemPayWalletSelectorComponent
    }
}