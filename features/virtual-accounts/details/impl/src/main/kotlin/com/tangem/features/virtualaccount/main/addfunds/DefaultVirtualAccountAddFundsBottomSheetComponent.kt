package com.tangem.features.virtualaccount.main.addfunds

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.virtualaccount.details.component.VirtualAccountAddFundsBottomSheetComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultVirtualAccountAddFundsBottomSheetComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: VirtualAccountAddFundsBottomSheetComponent.Params,
) : VirtualAccountAddFundsBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: VirtualAccountAddFundsModel = getOrCreateModel(params = params)

    override fun dismiss() {
        model.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        VirtualAccountAddFundsBottomSheet(state = state)
    }

    @AssistedFactory
    interface Factory : VirtualAccountAddFundsBottomSheetComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: VirtualAccountAddFundsBottomSheetComponent.Params,
        ): DefaultVirtualAccountAddFundsBottomSheetComponent
    }
}