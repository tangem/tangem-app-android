package com.tangem.features.promobanners.impl.campaigns.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.models.PromoCampaignState
import com.tangem.domain.promo.usecase.GetPromoCampaignStateUseCase
import com.tangem.features.promobanners.impl.R
import com.tangem.features.promobanners.impl.campaigns.analytics.PromoCampaignsAnalyticsEvent
import com.tangem.features.promobanners.impl.campaigns.converters.CampaignIdConverter
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignsBottomSheetConfig
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignType
import com.tangem.features.promobanners.impl.campaigns.entity.toPromoCampaignId
import com.tangem.features.promobanners.impl.campaigns.service.CampaignsService
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class CampaignsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val campaignIdConverter: CampaignIdConverter,
    campaignsService: CampaignsService,
    private val getPromoCampaignStateUseCase: GetPromoCampaignStateUseCase,
    @GlobalUiMessageSender private val messageSender: UiMessageSender,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    val bottomSheetNavigation: SlotNavigation<CampaignsBottomSheetConfig> = SlotNavigation()

    init {
        campaignsService.campaignFlow
            .onEach { request ->
                resolveStartNavigation(
                    campaignType = campaignIdConverter.convert(request.campaignId),
                    userWalletId = request.userWalletId,
                )
            }
            .launchIn(modelScope)
    }

    private fun resolveStartNavigation(campaignType: CampaignType?, userWalletId: UserWalletId) {
        modelScope.launch {
            val config = if (campaignType == null) {
                CampaignsBottomSheetConfig.NotActive
            } else {
                checkCampaignState(campaignType, userWalletId)
            }

            config?.let { bottomSheetNavigation.activate(it) }
        }
    }

    private suspend fun checkCampaignState(
        campaignType: CampaignType,
        userWalletId: UserWalletId,
    ): CampaignsBottomSheetConfig? = getPromoCampaignStateUseCase.invoke(
        campaign = campaignType.toPromoCampaignId(),
        userWalletId = userWalletId,
    ).fold(
        ifLeft = { error ->
            TangemLogger.e("Error getting campaign ${campaignType.campaignId} state", error)
            messageSender.send(SnackbarMessage(message = resourceReference(R.string.common_unknown_error)))
            null
        },
        ifRight = { campaignState ->
            when (campaignState) {
                is PromoCampaignState.Available -> CampaignsBottomSheetConfig.Activate(campaignType, userWalletId)
                is PromoCampaignState.NotActive -> CampaignsBottomSheetConfig.NotActive
            }
        },
    )

    fun onDismiss() {
        bottomSheetNavigation.dismiss()
    }

    fun onActivated(campaignType: CampaignType) {
        bottomSheetNavigation.activate(CampaignsBottomSheetConfig.Enrolled(campaignType))
    }

    fun onAlreadyActivated(campaignType: CampaignType) {
        analyticsEventHandler.send(PromoCampaignsAnalyticsEvent.AlreadyEnrolledScreenOpened())
        bottomSheetNavigation.activate(CampaignsBottomSheetConfig.AlreadyActivated(campaignType = campaignType))
    }
}