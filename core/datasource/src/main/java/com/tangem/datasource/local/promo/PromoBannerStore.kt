package com.tangem.datasource.local.promo

import com.tangem.datasource.api.promotion.models.PromoBannerResponse

interface PromoBannerStore {

    suspend fun getSyncOrNull(promoId: String): PromoBannerResponse?

    suspend fun store(promoId: String, promoBanner: PromoBannerResponse)
}