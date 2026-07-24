package com.tangem.features.promobanners.impl.campaigns.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.SpacerH32
import com.tangem.core.ui.components.bottomsheets.LocalBottomSheetContentScrollable
import com.tangem.core.ui.components.bottomsheets.LocalTangemBottomSheetContentBottomInset
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
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

        TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
            config = TangemBottomSheetConfig(
                isShown = activeChild != null,
                onDismissRequest = model::onDismiss,
                content = TangemBottomSheetConfigContent.Empty,
            ),
            containerColor = TangemTheme.colors3.bg.secondary,
            type = TangemBottomSheetType.Modal,
            onBack = model::onDismiss,
            title = {
                displayedChild?.Title()
            },
            content = {
                val bottomInset = LocalTangemBottomSheetContentBottomInset.current
                val bottomReserve = if (bottomInset > 0.dp) bottomInset else 16.dp
                val scrollState = rememberScrollState()
                val scrollableSignal = LocalBottomSheetContentScrollable.current

                if (scrollableSignal != null) {
                    LaunchedEffect(scrollState) {
                        snapshotFlow { scrollState.canScrollForward || scrollState.canScrollBackward }
                            .collect { canScroll -> scrollableSignal.value = canScroll }
                    }
                }

                Column(modifier = Modifier.verticalScroll(state = scrollState)) {
                    Box(modifier = Modifier.animateContentSize()) {
                        displayedChild?.Content(modifier = Modifier)
                    }

                    if (scrollableSignal?.value != true) SpacerH32()

                    Spacer(modifier = Modifier.height(bottomReserve))
                }
            },
            footer = {
                Box(modifier = Modifier.padding(12.dp)) {
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