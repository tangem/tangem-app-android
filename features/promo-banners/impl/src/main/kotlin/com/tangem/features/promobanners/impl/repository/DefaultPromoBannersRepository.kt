package com.tangem.features.promobanners.impl.repository

import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.promobanners.DismissPromoBannerRequest
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.features.promobanners.api.PromoBannersBlockComponent.Placeholder
import com.tangem.features.promobanners.impl.converters.PromoBannerDisplayDTOConverter
import com.tangem.features.promobanners.impl.model.PromoBannerDisplay
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

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

    override suspend fun getBanners(
        walletId: String,
        placeholder: Placeholder,
        locale: String,
    ): List<PromoBannerDisplay> {
        val key = BannersCacheKey(walletId, placeholder)

        cache.getSyncOrNull()?.get(key)?.let { return it }

        val banners = withContext(dispatchers.io) {
            tangemTechApi.getPromoBannerDisplays(
                walletId = walletId,
                placeholder = placeholder.toApiValue(),
                locale = locale,
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

    override suspend fun dismissBanner(walletId: String, displayId: String) {
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
                isDismissed = true,
            )
            tangemTechApi.dismissPromoBannerDisplay(displayId, request).getOrThrow()
        }
    }

    private fun Placeholder.toApiValue(): String = when (this) {
        Placeholder.MAIN -> "main"
        Placeholder.FEED -> "shtorka"
    }
}