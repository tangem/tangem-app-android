package com.tangem.data.promo.converter

import com.tangem.datasource.api.promotion.models.PromotionsResponse.PromotionDto.All
import com.tangem.domain.promo.models.PromoCampaignId
import com.tangem.domain.promo.models.PromoCampaignState
import com.tangem.domain.promo.models.PromoPayoutToken
import com.tangem.domain.promo.models.PromoTimeline
import kotlinx.datetime.Instant

internal object PromoCampaignConverter {

    fun toAvailable(campaign: PromoCampaignId, all: All): PromoCampaignState.Available {
        return PromoCampaignState.Available(
            campaign = campaign,
            payoutTokens = all.tokens.orEmpty().map { token ->
                PromoPayoutToken(
                    tokenId = token.tokenId,
                    tokenAddress = token.tokenAddress,
                    tokenSymbol = token.tokenSymbol,
                    tokenName = token.tokenName,
                    networkId = token.networkId,
                    decimals = token.decimals,
                )
            },
            timeline = PromoTimeline(
                start = Instant.parse(all.timeline.start),
                end = Instant.parse(all.timeline.end),
            ),
        )
    }
}