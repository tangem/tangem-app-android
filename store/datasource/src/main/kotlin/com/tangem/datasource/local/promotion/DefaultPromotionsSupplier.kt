package com.tangem.datasource.local.promotion

import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.promotion.models.PromotionsResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class DefaultPromotionsSupplier(
    private val tangemApi: TangemTechApi,
    private val store: RuntimeSharedStore<Map<UserWalletId, PromotionsResponse>>,
    private val dispatchers: CoroutineDispatcherProvider,
) : PromotionsSupplier {

    override suspend fun getPromotions(userWalletId: UserWalletId, forceRefresh: Boolean): PromotionsResponse {
        if (!forceRefresh) {
            store.getSyncOrNull()?.get(userWalletId)?.let { return it }
        }
        val fresh = withContext(dispatchers.io) {
            tangemApi.getPromotions(walletId = userWalletId.stringValue).getOrThrow()
        }
        store.update(emptyMap()) { it + (userWalletId to fresh) }
        return fresh
    }
}