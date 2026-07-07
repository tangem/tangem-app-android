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
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenComponent
import com.tangem.features.promobanners.api.swapcashback.CampaignsComponent
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignsBottomSheetConfig
import com.tangem.features.promobanners.impl.campaigns.model.ActivateCampaignsModel
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
        serializer = CampaignsBottomSheetConfig.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: CampaignsBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        val context = childByContext(componentContext)
        return when (config) {
            CampaignsBottomSheetConfig.NotActive -> NotActiveCampaignBottomSheetComponent(
                onDismissRequest = model::onDismiss,
            )
            is CampaignsBottomSheetConfig.Enrolled -> CampaignEnrolledBottomSheetComponent(
                campaignType = config.campaignType,
                onDismissRequest = model::onDismiss,
            )
            is CampaignsBottomSheetConfig.Activate -> ActivateCampaignBottomSheetComponent(
                appComponentContext = context,
                chooseTokenComponentFactory = chooseTokenComponentFactory,
                params = ActivateCampaignsModel.Params(
                    campaignType = config.campaignType,
                    onDismiss = model::onDismiss,
                    onActivated = model::onActivated,
                ),
            )
        }
    }

    @AssistedFactory
    interface Factory : CampaignsComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultCampaignsComponent
    }
}