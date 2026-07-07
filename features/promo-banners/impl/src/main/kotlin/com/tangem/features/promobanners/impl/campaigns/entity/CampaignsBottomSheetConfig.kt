package com.tangem.features.promobanners.impl.campaigns.entity

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import kotlinx.serialization.Serializable

@Serializable
internal sealed class CampaignsBottomSheetConfig {

    @Serializable
    data object NotActive : CampaignsBottomSheetConfig()

    @Serializable
    data class Enrolled(
        val campaignType: CampaignType,
    ) : CampaignsBottomSheetConfig()

    @Serializable
    data class Activate(
        val campaignType: CampaignType,
    ) : CampaignsBottomSheetConfig()

    @Serializable
    data class AlreadyActivated(
        val campaignType: CampaignType,
        val appCurrency: AppCurrency,
        val account: Account?,
        val currency: CryptoCurrencyStatus,
    ) : CampaignsBottomSheetConfig()
}