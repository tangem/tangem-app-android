package com.tangem.features.swap.v2.impl.choosetoken.fromSupported

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.swap.v2.api.choosetoken.SwapChooseTokenNetworkComponent
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.model.SwapChooseTokenNetworkModel
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.ui.SwapChooseTokenNetworkBottomSheet
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultSwapChooseTokenNetworkComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: SwapChooseTokenNetworkComponent.Params,
) : SwapChooseTokenNetworkComponent, AppComponentContext by context {

    private val model: SwapChooseTokenNetworkModel = getOrCreateModel(params)

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()

        SwapChooseTokenNetworkBottomSheet(
            config = state.bottomSheetConfig,
        )
    }

    @AssistedFactory
    interface Factory : SwapChooseTokenNetworkComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: SwapChooseTokenNetworkComponent.Params,
        ): DefaultSwapChooseTokenNetworkComponent
    }
}