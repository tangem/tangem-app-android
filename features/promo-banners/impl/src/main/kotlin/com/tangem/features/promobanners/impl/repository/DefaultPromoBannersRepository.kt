package com.tangem.features.promobanners.impl.repository

import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.promobanners.DismissPromoBannerRequest
import com.tangem.features.promobanners.impl.converters.PromoBannerDisplayDTOConverter
import com.tangem.features.promobanners.impl.model.PromoBannerDisplay
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultPromoBannersRepository @Inject constructor(
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : PromoBannersRepository {

    private val converter = PromoBannerDisplayDTOConverter()

    override suspend fun getBanners(walletId: String, placeholder: String, locale: String): List<PromoBannerDisplay> =
        withContext(dispatchers.io) {
            tangemTechApi.getPromoBannerDisplays(
                walletId = walletId,
                placeholder = placeholder,
                locale = locale,
            ).getOrThrow()
                .items
                .map(converter::convert)
                .sortedBy { it.priority.order }
        }

    override suspend fun dismissBanner(walletId: String, displayId: String) {
        withContext(dispatchers.io) {
            val request = DismissPromoBannerRequest(
                walletId = walletId,
                isDismissed = true,
            )
            tangemTechApi.dismissPromoBannerDisplay(displayId, request).getOrThrow()
        }
    }
}