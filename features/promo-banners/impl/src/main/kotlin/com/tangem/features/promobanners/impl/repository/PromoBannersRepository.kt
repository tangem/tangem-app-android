package com.tangem.features.promobanners.impl.repository

import com.tangem.features.promobanners.api.PromoBannersBlockComponent.Placeholder
import com.tangem.features.promobanners.impl.model.PromoBannerDisplay

internal interface PromoBannersRepository {

    suspend fun getBanners(
        walletId: String,
        placeholder: Placeholder,
        languageISOCode: String,
    ): List<PromoBannerDisplay>

    suspend fun dismissBanner(walletId: String, displayId: Int)
}