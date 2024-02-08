package com.tangem.data.promo

import com.tangem.data.promo.converters.PromoResponseConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.promo.PromoBanner
import com.tangem.domain.tokens.repository.PromoRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching

internal class DefaultPromoRepository(
    private val tangemApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : PromoRepository {

    private val promoResponseConverter = PromoResponseConverter()
    override suspend fun getChangellyPromoBanner(): PromoBanner? {
        return runCatching(dispatchers.io) {
            promoResponseConverter.convert(
                tangemApi.getPromotionInfo(CHANGELLY_NAME)
                    .getOrThrow(),
            )
        }.getOrNull()
    }

    private companion object {
        private const val CHANGELLY_NAME = "changelly"
    }
}
