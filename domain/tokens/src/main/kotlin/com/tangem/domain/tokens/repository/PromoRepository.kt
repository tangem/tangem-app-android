package com.tangem.domain.tokens.repository

import com.tangem.domain.promo.PromoBanner

interface PromoRepository {

    suspend fun getChangellyPromoBanner(): PromoBanner?

    suspend fun getOkxPromoBanner(): PromoBanner?

    suspend fun getRingPromoBanner(): PromoBanner?
}