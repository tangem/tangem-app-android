package com.tangem.data.marketing

import arrow.core.Either
import com.tangem.data.marketing.converter.MarketingCampaignConverter
import com.tangem.data.marketing.store.MarketingCampaignsCacheStore
import com.tangem.data.marketing.store.MarketingDismissStore
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ETAG_HEADER
import com.tangem.datasource.api.marketing.MarketingApi
import com.tangem.datasource.api.marketing.models.MarketingCampaignsCacheEntry
import com.tangem.datasource.api.marketing.models.MarketingCampaignsResponse
import com.tangem.domain.marketing.MarketingRepository
import com.tangem.domain.marketing.models.MarketingCampaign
import com.tangem.domain.marketing.models.MarketingScreen
import com.tangem.utils.SupportedLanguages
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class DefaultMarketingRepository(
    private val marketingApi: MarketingApi,
    private val cacheStore: MarketingCampaignsCacheStore,
    private val dismissStore: MarketingDismissStore,
    private val converter: MarketingCampaignConverter,
    private val dispatchers: CoroutineDispatcherProvider,
) : MarketingRepository {

    override suspend fun getCampaigns(screen: MarketingScreen): Either<Throwable, List<MarketingCampaign>> =
        withContext(dispatchers.io) {
            Either.catch {
                val isCacheable = screen.type.isCacheable
                val cached = if (isCacheable) cacheStore.get(screen.type.value) else null

                when (val response = requestCampaigns(screen, eTag = cached?.eTag)) {
                    is ApiResponse.Success -> {
                        if (isCacheable) {
                            // eTag may be null if the server omits it; we still cache the body for the 5xx
                            // fallback path. A null eTag simply means the next request sends no If-None-Match
                            // (Retrofit omits null headers) and receives a fresh 200.
                            val eTag = response.headers[ETAG_HEADER]?.firstOrNull()
                            cacheStore.store(screen.type.value, MarketingCampaignsCacheEntry(eTag, response.data))
                        }
                        convert(response.data)
                    }
                    is ApiResponse.Error -> handleError(cached)
                }
            }
        }

    override suspend fun getDismissedBannerIds(): Set<Int> = dismissStore.getDismissedIds()

    override suspend fun dismissBanner(campaignId: Int) = dismissStore.dismiss(campaignId)

    private suspend fun requestCampaigns(
        screen: MarketingScreen,
        eTag: String?,
    ): ApiResponse<MarketingCampaignsResponse> {
        val language = SupportedLanguages.getCurrentSupportedLanguageCode()
        return when (screen) {
            is MarketingScreen.Swap -> marketingApi.getCampaigns(
                type = screen.type.value,
                language = language,
                fromNetwork = screen.fromNetwork,
                fromContractAddress = screen.fromContractAddress,
                toNetwork = screen.toNetwork,
                toContractAddress = screen.toContractAddress,
            )
            is MarketingScreen.Onramp -> marketingApi.getCampaigns(
                type = screen.type.value,
                language = language,
                fromFiat = screen.fromFiat,
                toNetwork = screen.toNetwork,
                toToken = screen.toToken,
            )
            is MarketingScreen.TokenDetails,
            is MarketingScreen.TokenMarkets,
            is MarketingScreen.Staking,
            is MarketingScreen.Yield,
            -> marketingApi.getCampaigns(type = screen.type.value, language = language, eTag = eTag)
        }
    }

    /**
     * All error cases (304 not-modified, 5xx, network failure) degrade gracefully to the cached
     * response. Returning an empty list when there is no cache is intentional — "no banner" is a
     * normal state, not an error the caller needs to handle.
     */
    private fun handleError(cached: MarketingCampaignsCacheEntry?): List<MarketingCampaign> {
        return cached?.response?.let(::convert).orEmpty()
    }

    private fun convert(response: MarketingCampaignsResponse): List<MarketingCampaign> =
        converter.convertListIgnoreErrors(response.campaigns)
}