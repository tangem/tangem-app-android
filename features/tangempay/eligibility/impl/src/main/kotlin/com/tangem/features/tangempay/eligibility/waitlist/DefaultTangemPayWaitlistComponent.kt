package com.tangem.features.tangempay.eligibility.waitlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.tangempay.eligibility.TangemPayWaitlistComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayWaitlistComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: TangemPayWaitlistComponent.Params,
) : TangemPayWaitlistComponent, AppComponentContext by context {

    private val model: TangemPayWaitlistModel = getOrCreateModel(params)

    override fun dismiss() {
        model.dismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        TangemPayWaitlistBottomSheet(state = state)
    }

    @AssistedFactory
    interface Factory : TangemPayWaitlistComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayWaitlistComponent.Params,
        ): DefaultTangemPayWaitlistComponent
    }
}