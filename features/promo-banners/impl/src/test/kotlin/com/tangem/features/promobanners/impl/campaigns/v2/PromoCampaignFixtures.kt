package com.tangem.features.promobanners.impl.campaigns.v2

import com.tangem.domain.promo.models.PromoCampaign
import com.tangem.domain.promo.models.PromoCampaignStatus
import com.tangem.domain.promo.models.PromoCampaignContent
import com.tangem.domain.promo.models.PromoDescriptionBlock
import com.tangem.domain.promo.models.PromoEligibility
import com.tangem.domain.promo.models.PromoLink
import com.tangem.domain.promo.models.PromoMatchMode
import com.tangem.domain.promo.models.PromoPayout
import com.tangem.domain.promo.models.PromoPayoutToken
import com.tangem.domain.promo.models.PromoTimeline
import kotlinx.datetime.Instant

internal const val PROMO_IMAGE_URL = "https://tangem.com/promo.png"
internal const val PROMO_TNC_URL = "https://tangem.com/tnc"

internal fun createPromoCampaign(
    name: String = "whale-swap",
    id: Int = 1,
    blocks: List<PromoDescriptionBlock> = emptyList(),
    learnMoreLink: PromoLink? = null,
    isPayoutNeeded: Boolean = false,
    payoutTokens: List<PromoPayoutToken> = emptyList(),
    isAlreadyEnrolled: Boolean = false,
    status: PromoCampaignStatus = PromoCampaignStatus.Active,
    eligibility: PromoEligibility = PromoEligibility.Public,
) = PromoCampaign(
    id = id,
    name = name,
    status = status,
    timeline = PromoTimeline(start = Instant.fromEpochSeconds(0), end = Instant.fromEpochSeconds(1)),
    isAlreadyEnrolled = isAlreadyEnrolled,
    eligibility = eligibility,
    payout = PromoPayout(isNeeded = isPayoutNeeded, tokens = payoutTokens),
    matchMode = PromoMatchMode.Strict,
    content = PromoCampaignContent(
        nameKey = "Whale cashback",
        description = blocks,
        imageUrl = PROMO_IMAGE_URL,
        learnMoreLink = learnMoreLink,
        tncLink = PromoLink(textKey = "T&C", url = PROMO_TNC_URL),
        actionButtonNameKey = "Enroll",
    ),
)