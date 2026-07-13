package com.tangem.domain.marketing

import com.tangem.domain.marketing.models.MarketingScreenType
import kotlinx.coroutines.CancellationException

/**
 * Warms the session cache for background campaign types shown outside a dedicated screen entry
 * (token details & markets). Toggle-gated; failures are swallowed (fire-and-forget from the main screen).
 */
class WarmUpMarketingCampaignsUseCase(
    private val repository: MarketingRepository,
    private val featureToggles: MarketingFeatureToggles,
) {

    suspend operator fun invoke() {
        if (!featureToggles.isMarketingBannersEnabled) return
        WARMED_TYPES.forEach { type ->
            try {
                repository.prefetchBackgroundCampaigns(type)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // fire-and-forget warm-up: ignore, next screen open retries
            }
        }
    }

    private companion object {
        val WARMED_TYPES = listOf(MarketingScreenType.TOKEN_DETAILS, MarketingScreenType.TOKEN_MARKETS)
    }
}