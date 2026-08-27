package com.tangem.data.marketing

import arrow.core.Either
import com.tangem.data.marketing.converter.MarketingCampaignConverter
import com.tangem.data.marketing.store.MarketingCampaignsCacheStore
import com.tangem.data.marketing.store.MarketingDismissStore
import com.tangem.core.remote.response.ApiResponse
import com.tangem.datasource.api.common.response.ETAG_HEADER
import com.tangem.datasource.api.marketing.models.MarketingCampaignsCacheEntry
import com.tangem.datasource.api.marketing.models.MarketingCampaignsResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.marketing.MarketingRepository
import com.tangem.domain.marketing.models.MarketingCampaign
import com.tangem.domain.marketing.models.MarketingScreen
import com.tangem.domain.marketing.models.MarketingScreenType
import com.tangem.utils.SupportedLanguages
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class DefaultMarketingRepository(
    private val tangemTechApi: TangemTechApi,
    private val cacheStore: MarketingCampaignsCacheStore,
    private val dismissStore: MarketingDismissStore,
    private val converter: MarketingCampaignConverter,
    private val dispatchers: CoroutineDispatcherProvider,
) : MarketingRepository {

    // In-memory per-session cache for background (cacheable) types. Serves repeated reads within a session
    // without hitting the network; DataStore ETag cache remains the cross-session layer inside fetchAndCacheByType.
    private val sessionCache = MutableStateFlow<Map<MarketingScreenType, List<MarketingCampaign>>>(emptyMap())
    private val cacheMutex = Mutex()

    override suspend fun getCampaigns(screen: MarketingScreen): Either<Throwable, List<MarketingCampaign>> =
        withContext(dispatchers.io) {
            Either.catch {
                if (screen.type.isCacheable) {
                    loadCacheableByType(screen.type)
                } else {
                    // swap/onramp — always fresh, never cached
                    when (val response = requestCampaigns(screen, eTag = null)) {
                        is ApiResponse.Success -> convert(response.data)
                        is ApiResponse.Error -> emptyList()
                    }
                }
            }
        }

    override suspend fun prefetchBackgroundCampaigns(type: MarketingScreenType) {
        if (!type.isCacheable) return
        try {
            loadCacheableByType(type)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Fire-and-forget warm-up: failures are non-fatal, the next getCampaigns() call will retry.
        }
    }

    override suspend fun getDismissedBannerIds(): Set<Int> = dismissStore.getDismissedIds()

    override suspend fun dismissBanner(campaignId: Int) = dismissStore.dismiss(campaignId)

    private suspend fun loadCacheableByType(type: MarketingScreenType): List<MarketingCampaign> {
        sessionCache.value[type]?.let { return it }
        return cacheMutex.withLock {
            sessionCache.value[type]?.let { return@withLock it } // double-check under lock
            val result = fetchAndCacheByType(type)
            // Only cache authoritative results. A pure error fallback (error + no DataStore cache -> null)
            // must NOT poison the session cache, so a later screen open still retries the network.
            if (result != null) {
                sessionCache.update { it + (type to result) }
            }
            result.orEmpty()
        }
    }

    private suspend fun fetchAndCacheByType(type: MarketingScreenType): List<MarketingCampaign>? {
        val cached = cacheStore.get(type.value)
        return when (val response = requestByType(type, eTag = cached?.eTag)) {
            is ApiResponse.Success -> {
                // eTag may be null if the server omits it; we still cache the body for the 5xx
                // fallback path. A null eTag simply means the next request sends no If-None-Match
                // (Retrofit omits null headers) and receives a fresh 200.
                val eTag = response.headers[ETAG_HEADER]?.firstOrNull()
                cacheStore.store(type.value, MarketingCampaignsCacheEntry(eTag, response.data))
                convert(response.data) // authoritative (may be empty = real "no banners")
            }
            // Cached fallback is authoritative-ish; null when there is nothing cached (do not session-cache).
            is ApiResponse.Error -> cached?.response?.let(::convert)
        }
    }

    private suspend fun requestByType(
        type: MarketingScreenType,
        eTag: String?,
    ): ApiResponse<MarketingCampaignsResponse> {
        return tangemTechApi.getMarketingCampaigns(
            type = type.value,
            language = SupportedLanguages.getCurrentSupportedLanguageCode(),
            eTag = eTag,
        )
    }

    private suspend fun requestCampaigns(
        screen: MarketingScreen,
        eTag: String?,
    ): ApiResponse<MarketingCampaignsResponse> {
        val language = SupportedLanguages.getCurrentSupportedLanguageCode()
        return when (screen) {
            is MarketingScreen.Swap -> tangemTechApi.getMarketingCampaigns(
                type = screen.type.value,
                language = language,
                fromNetwork = screen.fromNetwork,
                // Omit the contract for a native coin (blank) — Retrofit drops null query params.
                fromContractAddress = screen.fromContractAddress.ifBlank { null },
                toNetwork = screen.toNetwork,
                toContractAddress = screen.toContractAddress.ifBlank { null },
            )
            is MarketingScreen.Onramp -> tangemTechApi.getMarketingCampaigns(
                type = screen.type.value,
                language = language,
                fromFiat = screen.fromFiat,
                toNetwork = screen.toNetwork,
                toContractAddress = screen.toContractAddress.ifBlank { null },
            )
            is MarketingScreen.TokenDetails,
            is MarketingScreen.TokenMarkets,
            is MarketingScreen.Staking,
            is MarketingScreen.Yield,
            -> requestByType(screen.type, eTag)
        }
    }

    private fun convert(response: MarketingCampaignsResponse): List<MarketingCampaign> =
        converter.convertListIgnoreErrors(response.campaigns) { throwable ->
            TangemLogger.w("Skipped invalid marketing campaign: ${throwable.message}")
        }
}