package com.tangem.features.promobanners.impl.campaigns.v2

import com.tangem.domain.promo.models.PromoCampaign
import com.tangem.domain.promo.models.PromoCampaignContent
import com.tangem.domain.promo.models.PromoDescriptionBlock
import com.tangem.domain.promo.models.PromoLink
import com.tangem.domain.promo.models.PromoMatchMode
import com.tangem.domain.promo.models.PromoPayout
import com.tangem.domain.promo.models.PromoPayoutToken

internal const val PROMO_IMAGE_URL = "https://tangem.com/promo.png"
internal const val PROMO_TNC_URL = "https://tangem.com/tnc"

internal fun createPromoCampaign(
    name: String = "whale-swap",
    id: Int = 1,
    blocks: List<PromoDescriptionBlock> = emptyList(),
    summary: String? = null,
    isPayoutNeeded: Boolean = false,
    payoutTokens: List<PromoPayoutToken> = emptyList(),
    isAlreadyEnrolled: Boolean = false,
    matchMode: PromoMatchMode = PromoMatchMode.Strict,
) = PromoCampaign(
    id = id,
    name = name,
    isAlreadyEnrolled = isAlreadyEnrolled,
    payout = PromoPayout(isNeeded = isPayoutNeeded, tokens = payoutTokens),
    matchMode = matchMode,
    content = PromoCampaignContent(
        name = "Whale cashback",
        summary = summary,
        description = blocks,
        imageUrl = PROMO_IMAGE_URL,
        tncLink = PromoLink(text = "T&C", url = PROMO_TNC_URL),
        actionButtonName = "Enroll",
    ),
)