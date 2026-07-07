package com.tangem.features.promobanners.impl.campaigns.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenComponent
import com.tangem.features.promobanners.impl.campaigns.model.ActivateCampaignsModel
import com.tangem.features.promobanners.impl.campaigns.ui.ActivateCampaignContent

internal class ActivateCampaignBottomSheetComponent(
    appComponentContext: AppComponentContext,
    chooseTokenComponentFactory: ChooseTokenComponent.Factory,
    params: ActivateCampaignsModel.Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: ActivateCampaignsModel = getOrCreateModel(params)

    private val chooseTokenComponent: ChooseTokenComponent = chooseTokenComponentFactory.create(
        context = child(key = "swapCashbackChooseToken"),
        params = ChooseTokenComponent.Params(bridge = model.bridge),
    )

    override fun dismiss() = model.onDismiss()

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()

        ActivateCampaignContent(
            state = state,
            onSelectTokenClick = model::onSelectTokenClick,
            onEnrollClick = model::onEnrollClick,
            onLearnMoreClick = { /* [REDACTED_TODO_COMMENT] */ },
            onDismiss = ::dismiss,
        )

        if (state.isChoosingToken) {
            ChooseTokenBottomSheet()
        }
    }

    @Composable
    private fun ChooseTokenBottomSheet() {
        TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = model::onChooseTokenDismiss,
                content = TangemBottomSheetConfigContent.Empty,
            ),
            onBack = model::onChooseTokenDismiss,
        ) {
            chooseTokenComponent.Content(modifier = Modifier.fillMaxWidth())
        }
    }
}