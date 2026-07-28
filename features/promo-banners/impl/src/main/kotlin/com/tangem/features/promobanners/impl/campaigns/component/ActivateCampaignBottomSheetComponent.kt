package com.tangem.features.promobanners.impl.campaigns.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignType
import com.tangem.features.promobanners.impl.campaigns.model.ActivateCampaignsModel
import com.tangem.features.promobanners.impl.campaigns.ui.ActivateCampaignContent
import com.tangem.features.promobanners.impl.campaigns.ui.ActivateCampaignFooter

internal class ActivateCampaignBottomSheetComponent(
    appComponentContext: AppComponentContext,
    chooseTokenComponentFactory: ChooseTokenComponent.Factory,
    private val params: Params,
    val onDismiss: () -> Unit,
) : ComposableModularContentComponent, AppComponentContext by appComponentContext {

    private val model: ActivateCampaignsModel = getOrCreateModel(params)

    private val chooseTokenComponent: ChooseTokenComponent = chooseTokenComponentFactory.create(
        context = child(key = "swapCashbackChooseToken"),
        params = ChooseTokenComponent.Params(bridge = model.bridge),
    )

    @Composable
    override fun Title() {
        TangemTopNavigation(
            windowInsets = WindowInsets(0),
            blurBackground = false,
            endButton = { TangemButton.Close(onClick = onDismiss) },
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        ActivateCampaignContent(um = state, modifier = modifier)

        if (state.isChoosingToken) {
            ChooseTokenBottomSheet(state.onChooseTokenDismiss)
        }
    }

    @Composable
    override fun Footer() {
        val state by model.uiState.collectAsStateWithLifecycle()

        ActivateCampaignFooter(
            footerUM = state.footerUM,
        )
    }

    @Composable
    private fun ChooseTokenBottomSheet(onChooseTokenDismiss: () -> Unit) {
        TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = onChooseTokenDismiss,
                content = TangemBottomSheetConfigContent.Empty,
            ),
            onBack = onChooseTokenDismiss,
            content = { chooseTokenComponent.Content(modifier = Modifier.fillMaxWidth()) },
        )
    }

    data class Params(
        val campaignType: CampaignType,
        val userWalletId: UserWalletId,
        val modelCallbacks: ActivateCampaignModelCallbacks,
    )

    interface ActivateCampaignModelCallbacks {
        val onActivated: (CampaignType) -> Unit
        val onAlreadyActivated: (CampaignType) -> Unit
    }
}