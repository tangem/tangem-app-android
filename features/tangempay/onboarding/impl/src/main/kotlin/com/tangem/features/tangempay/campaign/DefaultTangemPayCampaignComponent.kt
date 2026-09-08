package com.tangem.features.tangempay.campaign

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.backhandler.BackCallback
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.tangempay.components.TangemPayCampaignComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayCampaignComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: TangemPayCampaignComponent.Params,
) : TangemPayCampaignComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayCampaignModel = getOrCreateModel(params)

    init {
        backHandler.register(BackCallback { model.onClose() })
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        TangemPayCampaignScreen(state = state, modifier = modifier)
    }

    @AssistedFactory
    interface Factory : TangemPayCampaignComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayCampaignComponent.Params,
        ): DefaultTangemPayCampaignComponent
    }
}