package com.tangem.features.yield.supply.impl.promobottomsheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.yield.supply.api.promobottomsheet.YieldModePromoBottomSheetComponent
import com.tangem.features.yield.supply.impl.promobottomsheet.model.YieldModePromoBottomSheetModel
import com.tangem.features.yield.supply.impl.promobottomsheet.ui.YieldModePromoBottomSheet
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultYieldModePromoBottomSheetComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: YieldModePromoBottomSheetComponent.Params,
) : YieldModePromoBottomSheetComponent, AppComponentContext by context {

    private val model: YieldModePromoBottomSheetModel = getOrCreateModel(params)

    override fun dismiss() {
        model.dismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        YieldModePromoBottomSheet(state = state)
    }

    @AssistedFactory
    interface Factory : YieldModePromoBottomSheetComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: YieldModePromoBottomSheetComponent.Params,
        ): DefaultYieldModePromoBottomSheetComponent
    }
}