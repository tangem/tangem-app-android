package com.tangem.features.tangempay.campaign

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.tangempay.campaign.ui.TangemPayCampaignIssueCardScreen
import com.tangem.features.tangempay.components.TangemPayCampaignIssueCardComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayCampaignIssueCardComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: TangemPayCampaignIssueCardComponent.Params,
) : TangemPayCampaignIssueCardComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayCampaignIssueCardModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        BackHandler(onBack = model::onClose)
        TangemPayCampaignIssueCardScreen(state = state, modifier = modifier)
    }

    @AssistedFactory
    interface Factory : TangemPayCampaignIssueCardComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayCampaignIssueCardComponent.Params,
        ): DefaultTangemPayCampaignIssueCardComponent
    }
}