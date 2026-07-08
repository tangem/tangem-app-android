package com.tangem.domain.promo.models

enum class PromoCampaignId(val deeplinkId: Int, val slug: String) {
    WhaleSwapCashback(deeplinkId = 1, slug = "whale-swap-cashback"),
    ReactivationCashback(deeplinkId = 2, slug = "reactivation-cashback"),
    ;

    companion object {
        fun fromDeeplinkId(id: Int): PromoCampaignId? = entries.firstOrNull { it.deeplinkId == id }
        fun fromSlug(slug: String): PromoCampaignId? = entries.firstOrNull { it.slug == slug }
    }
}