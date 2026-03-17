package com.tangem.features.promobanners.impl.repository

import com.tangem.features.promobanners.impl.model.PromoBannerDisplay

internal interface PromoBannersRepository {

    suspend fun getBanners(walletId: String, placeholder: String, locale: String): List<PromoBannerDisplay>

    suspend fun dismissBanner(walletId: String, displayId: String)
}