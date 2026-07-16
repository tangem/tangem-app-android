package com.tangem.domain.promo.models

private const val CAMPAIGN_ID_WHALE = "whale-swap-cashback"
private const val CAMPAIGN_ID_REACTIVATION = "reactivation-cashback"

enum class PromoCampaignId(val slug: String) {
    WhaleSwapCashback(slug = CAMPAIGN_ID_WHALE),
    ReactivationCashback(slug = CAMPAIGN_ID_REACTIVATION),
    ;

    companion object {
        fun fromSlug(slug: String): PromoCampaignId? = entries.firstOrNull { it.slug == slug }
    }
}