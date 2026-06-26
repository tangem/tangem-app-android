package com.tangem.domain.marketing.models

import java.math.BigDecimal

data class MarketingCampaign(
    val id: Int,
    val type: MarketingScreenType,
    val priority: Int,
    val minAmount: BigDecimal?,
    val maxAmount: BigDecimal?,
    val providerIds: List<String>?,
    val banner: MarketingBanner,
    val targets: List<MarketingCampaignTarget>,
)