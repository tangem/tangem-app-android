package com.tangem.features.promobanners.impl.campaigns.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenComponent
import com.tangem.features.promobanners.api.swapcashback.CampaignsComponent
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

        // Sheets are always shown while composed; leaving composition plays the modal's collapse animation.
        bottomSheet.child?.instance?.Content(modifier = modifier)
    }

    private fun bottomSheetChild(
        config: CampaignsBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableContentComponent {
        val context = childByContext(componentContext)
        return when (config) {
            is CampaignsBottomSheetConfig.Campaign -> PromoCampaignBottomSheetComponent(
                appComponentContext = context,
                chooseTokenComponentFactory = chooseTokenComponentFactory,
                onDismiss = model::onDismiss,
                params = PromoCampaignBottomSheetComponent.Params(
                    campaign = config.campaign,
                    userWalletId = config.userWalletId,
                ),
            )
            CampaignsBottomSheetConfig.NotFound -> PromoCampaignMessageComponent(
                message = PromoCampaignMessages.notFound(),
                onDismiss = model::onDismiss,
            )
            is CampaignsBottomSheetConfig.AlreadyEnrolled -> PromoCampaignMessageComponent(
                message = PromoCampaignMessages.alreadyEnrolled(config.campaign),
                onDismiss = model::onDismiss,
            )
        }
    }

    @AssistedFactory
    interface Factory : CampaignsComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultCampaignsComponent
    }
}