package com.tangem.features.promobanners.impl.campaigns.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.DEFAULT_FOOTER_HEIGHT
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetWithFooter
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.extensions.rememberLastNonNull
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenComponent
import com.tangem.features.promobanners.api.swapcashback.CampaignsComponent
import com.tangem.features.promobanners.impl.campaigns.component.ActivateCampaignBottomSheetComponent.ActivateCampaignModelCallbacks
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignType
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignsBottomSheetConfig
import com.tangem.features.promobanners.impl.campaigns.model.CampaignsModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultCampaignsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Unit,
    private val chooseTokenComponentFactory: ChooseTokenComponent.Factory,
) : CampaignsComponent, AppComponentContext by appComponentContext {

    private val model: CampaignsModel = getOrCreateModel()

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        val activeChild = bottomSheet.child?.instance
        val displayedChild = rememberLastNonNull(activeChild)
        val footerExtraHeight by model.footerExtraHeightState.collectAsStateWithLifecycle()

        TangemModalBottomSheetWithFooter<TangemBottomSheetConfigContent.Empty>(
            config = TangemBottomSheetConfig(
                isShown = activeChild != null,
                onDismissRequest = model::onDismiss,
                content = TangemBottomSheetConfigContent.Empty,
            ),
            containerColor = TangemTheme.colors3.bg.primary,
            footerHeight = DEFAULT_FOOTER_HEIGHT + footerExtraHeight,
            onBack = model::onDismiss,
            title = {
                displayedChild?.Title()
            },
            content = {
                Box(modifier = Modifier.animateContentSize()) {
                    displayedChild?.Content(modifier = Modifier)
                }
            },
            footer = {
                Box(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(12.dp),
                ) {
                    displayedChild?.Footer()
                }
            },
        )
    }

    private fun bottomSheetChild(
        config: CampaignsBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableModularContentComponent {
        val context = childByContext(componentContext)
        return when (config) {
            CampaignsBottomSheetConfig.NotActive -> NotActiveCampaignBottomSheetComponent(
                onDismissRequest = model::onDismiss,
            )
            is CampaignsBottomSheetConfig.Enrolled -> CampaignEnrolledBottomSheetComponent(
                params = CampaignEnrolledBottomSheetComponent.Params(
                    campaignType = config.campaignType,
                ),
                onDismissRequest = model::onDismiss,
            )
            is CampaignsBottomSheetConfig.Activate -> ActivateCampaignBottomSheetComponent(
                appComponentContext = context,
                chooseTokenComponentFactory = chooseTokenComponentFactory,
                onDismiss = model::onDismiss,
                onFooterExtraHeightReady = model::onFooterExtraHeightReady,
                params = ActivateCampaignBottomSheetComponent.Params(
                    campaignType = config.campaignType,
                    userWalletId = config.userWalletId,
                    modelCallbacks = object : ActivateCampaignModelCallbacks {
                        override val onActivated: (CampaignType) -> Unit = model::onActivated
                        override val onAlreadyActivated: (CampaignType) -> Unit = model::onAlreadyActivated
                    },
                ),
            )
            is CampaignsBottomSheetConfig.AlreadyActivated -> CampaignAlreadyActivatedBottomSheetComponent(
                appComponentContext = context,
                params = CampaignAlreadyActivatedBottomSheetComponent.Params(
                    campaignType = config.campaignType,
                ),
                onDismiss = model::onDismiss,
            )
        }
    }

    @AssistedFactory
    interface Factory : CampaignsComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultCampaignsComponent
    }
}