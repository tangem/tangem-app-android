package com.tangem.features.promobanners.impl.campaigns.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.promobanners.impl.campaigns.converters.CampaignIdConverter
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignsBottomSheetConfig
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignType
import com.tangem.features.promobanners.impl.campaigns.service.CampaignsService
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@ModelScoped
internal class CampaignsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val campaignIdConverter: CampaignIdConverter,
    campaignsService: CampaignsService,
) : Model() {

    val bottomSheetNavigation: SlotNavigation<CampaignsBottomSheetConfig> = SlotNavigation()

    init {
        campaignsService.campaignFlow
            .onEach { campaignId -> resolveStartNavigation(campaignIdConverter.convert(campaignId)) }
            .launchIn(modelScope)
    }

    @Suppress("UnusedPrivateMember")
    private fun resolveStartNavigation(campaignType: CampaignType?) {
        val config = when (campaignType) {
            is CampaignType.ReactivationCashback -> checkReactivationCashbackCampaignState(campaignType)
            is CampaignType.WhaleSwapCashback -> checkWhaleSwapCashbackCampaignState(campaignType)
            null -> CampaignsBottomSheetConfig.NotActive
        }

        bottomSheetNavigation.activate(config)
    }

    // TODO
    private fun checkReactivationCashbackCampaignState(campaignType: CampaignType): CampaignsBottomSheetConfig {
        return CampaignsBottomSheetConfig.Activate(campaignType)
    }

    // TODO
    private fun checkWhaleSwapCashbackCampaignState(campaignType: CampaignType): CampaignsBottomSheetConfig {
        return CampaignsBottomSheetConfig.Activate(campaignType)
    }

    fun onDismiss() {
        bottomSheetNavigation.dismiss()
    }

    fun onActivated(campaignType: CampaignType) {
        bottomSheetNavigation.activate(CampaignsBottomSheetConfig.Enrolled(campaignType))
    }

    fun onAlreadyActivated(
        campaignType: CampaignType,
        appCurrency: AppCurrency,
        account: Account?,
        currency: CryptoCurrencyStatus,
    ) {
        bottomSheetNavigation.activate(
            CampaignsBottomSheetConfig.AlreadyActivated(
                campaignType = campaignType,
                appCurrency = appCurrency,
                account = account,
                currency = currency,
            ),
        )
    }
}