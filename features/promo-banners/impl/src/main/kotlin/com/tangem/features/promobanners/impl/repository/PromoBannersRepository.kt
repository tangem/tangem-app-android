package com.tangem.features.promobanners.impl.repository

import com.tangem.features.promobanners.api.PromoBannersBlockComponent.Placeholder
import com.tangem.features.promobanners.impl.model.PromoBannerDisplay
import kotlinx.coroutines.flow.Flow

internal interface PromoBannersRepository {

    suspend fun getBannersSync(
        walletId: String,
        placeholder: Placeholder,
        languageISOCode: String,
    ): List<PromoBannerDisplay>

    suspend fun getCachedBanners(walletId: String, placeholder: Placeholder): List<PromoBannerDisplay>?

    /** Refetches every placeholder already cached for [walletIds]; a failed one keeps its cached banners. */
    suspend fun refreshBanners(walletIds: Set<String>, languageISOCode: String)

    /** Emits the cached banners of [walletId] in [placeholder] whenever they change; never fetches. */
    fun getBanners(walletId: String, placeholder: Placeholder): Flow<List<PromoBannerDisplay>>

    suspend fun dismissBanner(walletId: String, displayId: Int)
}