package com.tangem.features.tangempay.campaign

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.campaign.ui.TangemPayCampaignScreen
import com.tangem.features.tangempay.components.TangemPayCampaignComponent
import com.tangem.features.tangempay.components.TangemPayCampaignIssueCardComponent
import com.tangem.features.tangempay.eligibility.TangemPayWalletSelectorComponent
import com.tangem.features.tangempay.ui.TangemPayOnboardingNavigation
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayCampaignComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: TangemPayCampaignComponent.Params,
    private val issueCardComponentFactory: TangemPayCampaignIssueCardComponent.Factory,
    private val walletSelectorComponentFactory: TangemPayWalletSelectorComponent.Factory,
) : TangemPayCampaignComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayCampaignModel = getOrCreateModel(params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = TangemPayOnboardingNavigation.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    private val childSlot = childSlot(
        source = model.childNavigation,
        serializer = TangemPayCampaignChild.serializer(),
        key = "campaignChild",
        handleBackButton = false,
        childFactory = ::child,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        val child by childSlot.subscribeAsState()
        val childComponent = child.child?.instance
        BackHandler(enabled = childComponent == null, onBack = model::onClose)
        if (childComponent != null) {
            childComponent.Content(modifier = modifier)
        } else {
            TangemPayCampaignScreen(state = state, modifier = modifier)
        }
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun child(child: TangemPayCampaignChild, componentContext: ComponentContext): ComposableContentComponent {
        return when (child) {
            is TangemPayCampaignChild.IssueCard -> issueCardComponentFactory.create(
                context = childByContext(componentContext),
                params = TangemPayCampaignIssueCardComponent.Params(
                    userWalletId = child.userWalletId,
                    campaignKey = params.campaignKey,
                    cardArtUrl = child.cardArtUrl,
                    feeAmount = child.feeAmount,
                    feeCurrency = child.feeCurrency,
                ),
            )
        }
    }

    private fun bottomSheetChild(
        navigation: TangemPayOnboardingNavigation,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        val context = childByContext(componentContext)
        return when (navigation) {
            is TangemPayOnboardingNavigation.WalletSelector -> walletSelectorComponentFactory.create(
                context = context,
                params = TangemPayWalletSelectorComponent.Params(listener = model, walletsIds = navigation.walletsIds),
            )
        }
    }

    @AssistedFactory
    interface Factory : TangemPayCampaignComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayCampaignComponent.Params,
        ): DefaultTangemPayCampaignComponent
    }
}