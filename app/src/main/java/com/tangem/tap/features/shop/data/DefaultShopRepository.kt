package com.tangem.tap.features.shop.data

import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.ShopResponse
import com.tangem.domain.common.extensions.withIOContext
import com.tangem.tap.features.shop.domain.ShopRepository
import com.tangem.tap.features.shop.domain.models.SalesProduct
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import timber.log.Timber
import java.util.Locale

/**
 * Default implementation of shop feature repository
 *
 * @property tangemTechApi TangemTech API
 * @property dispatchers   coroutine dispatchers provider
 *
[REDACTED_AUTHOR]
 */
internal class DefaultShopRepository(
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : ShopRepository {

    private val salesProductConverter = SalesProductConverter()

    override suspend fun isShopifyOrderingAvailable(): Boolean {
        return runCatching(dispatchers.io) { tangemTechApi.getShopInfo(name = SHOPIFY_NAME) }
            .fold(
                onSuccess = ShopResponse::isOrderingAvailable,
                onFailure = {
                    Timber.e("Server error. isShopifyOrderingAvailable returns default value (true)")
                    true
                },
            )
    }

    override suspend fun getSalesProductInfo(): List<SalesProduct> {
        return withIOContext {
            val salesInfo = tangemTechApi.getSalesInfo(locale = getLocaleName(), shops = SHOPIFY_NAME)
            salesProductConverter.convert(salesInfo)
        }
    }

    private fun getLocaleName(): String {
        return if (Locale.getDefault().language == "ru") {
            RU_LOCALE
        } else {
            EN_LOCALE
        }
    }

    private companion object {
        private const val SHOPIFY_NAME = "shopify"
        private const val RU_LOCALE = "ru"
        private const val EN_LOCALE = "en"
    }
}