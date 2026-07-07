package com.tangem.features.promobanners.impl.campaigns.entity

internal fun CampaignType.campaignName(): String = when (this) {
    // TODO([REDACTED_TASK_KEY]): source real campaign display names.
    is CampaignType.ReactivationCashback -> "Reactivation Cashback"
    is CampaignType.WhaleSwapCashback -> "Whale Swap Cashback"
}