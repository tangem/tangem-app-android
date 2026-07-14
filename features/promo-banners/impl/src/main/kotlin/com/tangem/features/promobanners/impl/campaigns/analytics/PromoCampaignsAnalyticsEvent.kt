package com.tangem.features.promobanners.impl.campaigns.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignType

internal sealed class PromoCampaignsAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Promotion", event = event, params = params) {

    class PromotionScreenOpened(campaignType: CampaignType) : PromoCampaignsAnalyticsEvent(
        event = "Promotion Screen Opened",
        params = mapOf("Screen" to campaignType.analyticsName),
    )

    class EnrollButtonClicked(
        campaignType: CampaignType,
        token: String,
        blockchain: String,
    ) : PromoCampaignsAnalyticsEvent(
        event = "Enroll Button Clicked",
        params = mapOf(
            "Campaign" to campaignType.analyticsName,
            "Token" to token,
            "Blockchain" to blockchain,
        ),
    )

    class AlreadyEnrolledScreenOpened : PromoCampaignsAnalyticsEvent(event = "Already Enrolled Screen Opened")
}

private val CampaignType.analyticsName: String
    get() = when (this) {
        is CampaignType.WhaleSwapCashback -> "Cashback"
        is CampaignType.ReactivationCashback -> "Reactivation"
    }