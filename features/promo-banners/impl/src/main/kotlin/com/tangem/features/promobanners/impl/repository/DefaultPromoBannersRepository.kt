package com.tangem.features.promobanners.impl.repository

import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.promobanners.DismissPromoBannerRequest
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.features.promobanners.api.PromoBannersBlockComponent.Placeholder
import com.tangem.features.promobanners.impl.converters.PromoBannerDisplayDTOConverter
import com.tangem.features.promobanners.impl.model.PromoBannerDisplay
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

internal typealias BannersCache = Map<BannersCacheKey, List<PromoBannerDisplay>>

internal data class BannersCacheKey(
    val walletId: String,
    val placeholder: Placeholder,
)

internal class DefaultPromoBannersRepository(
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val cache: RuntimeSharedStore<BannersCache>,
) : PromoBannersRepository {

    private val converter = PromoBannerDisplayDTOConverter()

    private val fetchMutexes = ConcurrentHashMap<BannersCacheKey, Mutex>()

    override suspend fun getBannersSync(
        walletId: String,
        placeholder: Placeholder,
        languageISOCode: String,
    ): List<PromoBannerDisplay> {
        val key = BannersCacheKey(walletId, placeholder)

        return lockOf(key).withLock {
            cache.getSyncOrNull()?.get(key) ?: fetch(key, languageISOCode)
        }
    }

    override suspend fun refreshBanners(walletIds: Set<String>, languageISOCode: String) {
        val keys = cache.getSyncOrNull()?.keys.orEmpty().filter { it.walletId in walletIds }

        keys.forEach { key ->
            runSuspendCatching { lockOf(key).withLock { fetch(key, languageISOCode) } }
                .onFailure { error -> TangemLogger.e("Failed to refresh promo banners for $key", error) }
        }
    }

    override fun getBanners(walletId: String, placeholder: Placeholder): Flow<List<PromoBannerDisplay>> {
        val key = BannersCacheKey(walletId, placeholder)

        return cache.get().mapNotNull { it[key] }.distinctUntilChanged()
    }

    override suspend fun getCachedBanners(walletId: String, placeholder: Placeholder): List<PromoBannerDisplay>? {
        return cache.getSyncOrNull()?.get(BannersCacheKey(walletId, placeholder))
    }

    override suspend fun dismissBanner(walletId: String, displayId: Int) {
        cache.update(default = emptyMap()) { current ->
            current.mapValues { (key, banners) ->
                if (key.walletId == walletId) {
                    banners.filterNot { it.id == displayId }
                } else {
                    banners
                }
            }
        }

        withContext(dispatchers.io) {
            val request = DismissPromoBannerRequest(
                walletId = walletId,
                status = DismissPromoBannerRequest.BannerDisplayStatus.DISMISSED,
            )
            tangemTechApi.dismissPromoBannerDisplay(displayId, request).getOrThrow()
        }
    }

    private fun lockOf(key: BannersCacheKey): Mutex = fetchMutexes.getOrPut(key, ::Mutex)

    private suspend fun fetch(key: BannersCacheKey, languageISOCode: String): List<PromoBannerDisplay> {
        val banners = withContext(dispatchers.io) {
            tangemTechApi.getPromoBannerDisplays(
                walletId = key.walletId,
                placeholder = key.placeholder.value,
                languageISOCode = languageISOCode,
            ).getOrThrow()
                .items
                .map(converter::convert)
                .sortedBy { it.priority.order }
        }

        cache.update(default = emptyMap()) { current ->
            current + (key to banners)
        }

        return banners
    }
}