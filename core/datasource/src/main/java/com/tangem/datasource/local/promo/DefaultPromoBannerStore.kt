package com.tangem.datasource.local.promo

import com.tangem.datasource.api.promotion.models.PromoBannerResponse
import com.tangem.datasource.local.datastore.RuntimeSharedStore

internal class DefaultPromoBannerStore(
    private val dataStore: RuntimeSharedStore<Map<String, PromoBannerResponse>>,
) : PromoBannerStore {

    override suspend fun getSyncOrNull(promoId: String): PromoBannerResponse? {
        return dataStore.getSyncOrNull()?.get(promoId)
    }

    override suspend fun store(promoId: String, promoBanner: PromoBannerResponse) {
        dataStore.update(emptyMap()) { current ->
            current + (promoId to promoBanner)
        }
    }
}