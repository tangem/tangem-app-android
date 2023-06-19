package com.tangem.tap.features.shop.data

import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.ShopResponse
import com.tangem.tap.features.shop.domain.ShopRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import timber.log.Timber

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

    override suspend fun isShopifyOrderingAvailable(): Boolean {
        return runCatching(dispatchers.io) { tangemTechApi.getShopInfo(name = SHOPIFY_NAME) }
            .fold(
                onSuccess = ShopResponse::isOrderingAvailable,
                onFailure = {
                    Timber.e("Server error. isShopifyOrderingAvailable returns default value (false)")
                    false
                },
            )
    }

    private companion object {
        const val SHOPIFY_NAME = "shopify"
    }
}