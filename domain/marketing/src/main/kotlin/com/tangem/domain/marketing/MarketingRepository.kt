package com.tangem.domain.marketing

import arrow.core.Either
import com.tangem.domain.marketing.models.MarketingCampaign
import com.tangem.domain.marketing.models.MarketingScreen
import com.tangem.domain.marketing.models.MarketingScreenType

interface MarketingRepository {

    /** Fetches campaigns for [screen]. Returns Right(emptyList()) when there is nothing to show (incl. 5xx without cache). */
    suspend fun getCampaigns(screen: MarketingScreen): Either<Throwable, List<MarketingCampaign>>

    /** Loads and caches campaigns for a background [type] into the in-memory session cache (fire-and-forget warm-up). */
    suspend fun prefetchBackgroundCampaigns(type: MarketingScreenType)

    /** Ids of campaigns whose banner the user has dismissed (stored client-side). */
    suspend fun getDismissedBannerIds(): Set<Int>

    suspend fun dismissBanner(campaignId: Int)
}