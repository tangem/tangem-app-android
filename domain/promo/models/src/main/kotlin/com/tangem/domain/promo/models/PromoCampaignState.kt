package com.tangem.domain.promo.models

sealed interface PromoCampaignState {

    val campaign: PromoCampaignId

    data class Available(
        override val campaign: PromoCampaignId,
        val payoutTokens: List<PromoPayoutToken>,
        val timeline: PromoTimeline,
    ) : PromoCampaignState

    data class Enrolled(
        override val campaign: PromoCampaignId,
        val tokenReward: TokenReward,
    ) : PromoCampaignState

    data class NotActive(
        override val campaign: PromoCampaignId,
    ) : PromoCampaignState
}