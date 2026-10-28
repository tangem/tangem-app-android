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
import com.tangem.domain.promo.usecase.GetPromoCampaignsUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.promobanners.impl.R
import com.tangem.features.promobanners.impl.campaigns.analytics.PromoCampaignsAnalyticsEvent.AlreadyEnrolledScreenOpened
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignsBottomSheetConfig
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
    campaignsService: CampaignsService,
    @GlobalUiMessageSender private val messageSender: UiMessageSender,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getPromoCampaignsUseCase: GetPromoCampaignsUseCase,
) : Model() {

    val bottomSheetNavigation: SlotNavigation<CampaignsBottomSheetConfig> = SlotNavigation()

    init {
        campaignsService.campaignFlow
            .onEach { request ->
                showPromoCampaign(campaignId = request.campaignId, userWalletId = request.userWalletId)
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

            getPromoCampaignsUseCase.invokeSync(userWallet).fold(
                ifLeft = { error ->
                    TangemLogger.e("Error getting campaign $campaignId", error)
                    showUnknownError()
                },
                ifRight = { campaigns ->
                    val campaign = campaigns.findByIdOrName(campaignId)
                    val config = when {
                        campaign == null -> CampaignsBottomSheetConfig.NotFound
                        campaign.isAlreadyEnrolled -> {
                            analyticsEventHandler.send(
                                AlreadyEnrolledScreenOpened(
                                    campaign = campaign.name,
                                    source = AlreadyEnrolledScreenOpened.Source.Entry,
                                ),
                            )
                            CampaignsBottomSheetConfig.AlreadyEnrolled(campaign)
                        }
                        else -> CampaignsBottomSheetConfig.Campaign(campaign, userWalletId)
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

    private fun showUnknownError() {
        messageSender.send(SnackbarMessage(message = resourceReference(R.string.common_unknown_error)))
    }

    fun onDismiss() {
        bottomSheetNavigation.dismiss()
    }
}