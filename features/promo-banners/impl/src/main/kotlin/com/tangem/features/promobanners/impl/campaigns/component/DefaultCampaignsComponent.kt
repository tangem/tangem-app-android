package com.tangem.features.promobanners.impl.campaigns.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
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
import com.tangem.core.ui.components.bottomsheets.*
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.extensions.rememberLastNonNull
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenComponent
import com.tangem.features.promobanners.api.swapcashback.CampaignsComponent
import com.tangem.features.promobanners.impl.campaigns.component.ActivateCampaignBottomSheetComponent.ActivateCampaignModelCallbacks
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignType
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignsBottomSheetConfig
import com.tangem.features.promobanners.impl.campaigns.model.CampaignsModel
import com.tangem.features.promobanners.impl.campaigns.v2.component.PromoCampaignBottomSheetComponent
import com.tangem.features.promobanners.impl.campaigns.v2.component.PromoCampaignMessageComponent
import com.tangem.features.promobanners.impl.campaigns.v2.converters.PromoCampaignMessages
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
        val activeSheet = bottomSheet.child?.instance
        val activeV1 = (activeSheet as? CampaignsSheet.V1)?.component
        val activeV2 = (activeSheet as? CampaignsSheet.V2)?.component

        V1BottomSheet(activeChild = activeV1, displayedChild = rememberLastNonNull(activeV1))

        // v2 sheets are always shown while composed; leaving composition plays the modal's collapse animation.
        activeV2?.Content(modifier = modifier)
    }

    @Composable
    private fun V1BottomSheet(
        activeChild: ComposableModularContentComponent?,
        displayedChild: ComposableModularContentComponent?,
    ) {
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
    ): CampaignsSheet {
        val context = childByContext(componentContext)
        return when (config) {
            is CampaignsBottomSheetConfig.V1 -> CampaignsSheet.V1(v1BottomSheetChild(config, context))
            is CampaignsBottomSheetConfig.V2 -> CampaignsSheet.V2(v2BottomSheetChild(config, context))
        }
    }

    private fun v1BottomSheetChild(
        config: CampaignsBottomSheetConfig.V1,
        context: AppComponentContext,
    ): ComposableModularContentComponent {
        return when (config) {
            CampaignsBottomSheetConfig.V1.NotActive -> NotActiveCampaignBottomSheetComponent(
                onDismissRequest = model::onDismiss,
            )
            is CampaignsBottomSheetConfig.V1.Enrolled -> CampaignEnrolledBottomSheetComponent(
                params = CampaignEnrolledBottomSheetComponent.Params(
                    campaignType = config.campaignType,
                ),
                onDismissRequest = model::onDismiss,
            )
            is CampaignsBottomSheetConfig.V1.Activate -> ActivateCampaignBottomSheetComponent(
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
            is CampaignsBottomSheetConfig.V1.AlreadyActivated -> CampaignAlreadyActivatedBottomSheetComponent(
                appComponentContext = context,
                params = CampaignAlreadyActivatedBottomSheetComponent.Params(
                    campaignType = config.campaignType,
                ),
                onDismiss = model::onDismiss,
            )
        }
    }

    private fun v2BottomSheetChild(
        config: CampaignsBottomSheetConfig.V2,
        context: AppComponentContext,
    ): ComposableContentComponent {
        return when (config) {
            is CampaignsBottomSheetConfig.V2.Campaign -> PromoCampaignBottomSheetComponent(
                appComponentContext = context,
                chooseTokenComponentFactory = chooseTokenComponentFactory,
                onDismiss = model::onDismiss,
                params = PromoCampaignBottomSheetComponent.Params(
                    campaign = config.campaign,
                    userWalletId = config.userWalletId,
                ),
            )
            CampaignsBottomSheetConfig.V2.NotFound -> PromoCampaignMessageComponent(
                message = PromoCampaignMessages.notFound(),
                onDismiss = model::onDismiss,
            )
            is CampaignsBottomSheetConfig.V2.AlreadyEnrolled -> PromoCampaignMessageComponent(
                message = PromoCampaignMessages.alreadyEnrolled(config.campaign),
                onDismiss = model::onDismiss,
            )
        }
    }

    /** v1 sheets fill the host [TangemBottomSheet] slot by slot; v2 sheets draw their whole body in a DS3 modal. */
    private sealed interface CampaignsSheet {
        data class V1(val component: ComposableModularContentComponent) : CampaignsSheet
        data class V2(val component: ComposableContentComponent) : CampaignsSheet
    }

    @AssistedFactory
    interface Factory : CampaignsComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultCampaignsComponent
    }
}