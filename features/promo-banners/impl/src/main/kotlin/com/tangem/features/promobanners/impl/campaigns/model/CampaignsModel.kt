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
import com.tangem.domain.promo.models.PromoCampaign
import com.tangem.domain.promo.models.PromoCampaignState
import com.tangem.domain.promo.usecase.GetPromoCampaignStateUseCase
import com.tangem.domain.promo.usecase.GetPromoCampaignsUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.promobanners.api.PromoBannersFeatureToggles
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

@Suppress("LongParameterList")
@ModelScoped
internal class CampaignsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val campaignIdConverter: CampaignIdConverter,
    campaignsService: CampaignsService,
    private val getPromoCampaignStateUseCase: GetPromoCampaignStateUseCase,
    @GlobalUiMessageSender private val messageSender: UiMessageSender,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val promoBannersFeatureToggles: PromoBannersFeatureToggles,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getPromoCampaignsUseCase: GetPromoCampaignsUseCase,
) : Model() {

    val bottomSheetNavigation: SlotNavigation<CampaignsBottomSheetConfig> = SlotNavigation()

    init {
        campaignsService.campaignFlow
            .onEach { request ->
                if (promoBannersFeatureToggles.isPromoCampaignsV2Enabled) {
                    showPromoCampaign(campaignId = request.campaignId, userWalletId = request.userWalletId)
                } else {
                    resolveStartNavigation(
                        campaignType = campaignIdConverter.convert(request.campaignId),
                        userWalletId = request.userWalletId,
                    )
                }
            }
            .launchIn(modelScope)
    }

    /** The campaigns are usually preloaded on start, so this is served from the per-wallet session cache. */
    private fun showPromoCampaign(campaignId: String, userWalletId: UserWalletId) {
        modelScope.launch {
            val userWallet = getUserWalletUseCase(userWalletId).getOrNull()
            if (userWallet == null) {
                TangemLogger.e("Error getting user wallet for campaign $campaignId")
                showUnknownError()
                return@launch
            }

            getPromoCampaignsUseCase(userWallet).fold(
                ifLeft = { error ->
                    TangemLogger.e("Error getting campaign $campaignId", error)
                    showUnknownError()
                },
                ifRight = { campaigns ->
                    val campaign = campaigns.findByIdOrName(campaignId)
                    val config = when {
                        campaign == null -> CampaignsBottomSheetConfig.V2.NotFound
                        campaign.isAlreadyEnrolled -> {
                            analyticsEventHandler.send(PromoCampaignsAnalyticsEvent.AlreadyEnrolledScreenOpened())
                            CampaignsBottomSheetConfig.V2.AlreadyEnrolled(campaign)
                        }
                        else -> CampaignsBottomSheetConfig.V2.Campaign(campaign, userWalletId)
                    }
                    bottomSheetNavigation.activate(config)
                },
            )
        }
    }

    /** The deeplink `campaignId` carries either the numeric id (banners) or the name; the id wins on a clash. */
    private fun List<PromoCampaign>.findByIdOrName(campaignId: String): PromoCampaign? {
        val byId = campaignId.toIntOrNull()?.let { id -> firstOrNull { it.id == id } }

        return byId ?: firstOrNull { it.name == campaignId }
    }

    private fun resolveStartNavigation(campaignType: CampaignType?, userWalletId: UserWalletId) {
        modelScope.launch {
            val config = if (campaignType == null) {
                CampaignsBottomSheetConfig.V1.NotActive
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
            showUnknownError()
            null
        },
        ifRight = { campaignState ->
            when (campaignState) {
                is PromoCampaignState.Available -> CampaignsBottomSheetConfig.V1.Activate(campaignType, userWalletId)
                is PromoCampaignState.NotActive -> CampaignsBottomSheetConfig.V1.NotActive
            }
        },
    )

    private fun showUnknownError() {
        messageSender.send(SnackbarMessage(message = resourceReference(R.string.common_unknown_error)))
    }

    fun onDismiss() {
        bottomSheetNavigation.dismiss()
    }

    fun onActivated(campaignType: CampaignType) {
        bottomSheetNavigation.activate(CampaignsBottomSheetConfig.V1.Enrolled(campaignType))
    }

    fun onAlreadyActivated(campaignType: CampaignType) {
        analyticsEventHandler.send(PromoCampaignsAnalyticsEvent.AlreadyEnrolledScreenOpened())
        bottomSheetNavigation.activate(CampaignsBottomSheetConfig.V1.AlreadyActivated(campaignType = campaignType))
    }
}