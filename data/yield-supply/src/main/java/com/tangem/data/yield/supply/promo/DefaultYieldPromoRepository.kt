package com.tangem.data.yield.supply.promo

import com.tangem.data.yield.supply.promo.converter.YieldBoostPromoConverter
import com.tangem.data.yield.supply.promo.converter.YieldBoostStatusConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.yieldsupply.promo.YieldBoostPromoStore
import com.tangem.datasource.local.yieldsupply.promo.YieldBoostStatusStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.YieldBoostPromo
import com.tangem.domain.yield.supply.models.YieldBoostStatus
import com.tangem.domain.yield.supply.promo.YieldPromoRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class DefaultYieldPromoRepository(
    private val tangemApi: TangemTechApi,
    private val promoStore: YieldBoostPromoStore,
    private val statusStore: YieldBoostStatusStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : YieldPromoRepository {

    override suspend fun getYieldBoostPromo(userWalletId: UserWalletId, forceRefresh: Boolean): YieldBoostPromo {
        if (!forceRefresh) {
            promoStore.getSyncOrNull(userWalletId)?.let { return it }
        }
        return try {
            val fresh = fetchPromo(userWalletId)
            promoStore.store(userWalletId, fresh)
            fresh
        } catch (e: Exception) {
            promoStore.getSyncOrNull(userWalletId) ?: throw e
        }
    }

    override suspend fun getYieldBoostStatus(userWalletId: UserWalletId, forceRefresh: Boolean): YieldBoostStatus {
        if (!forceRefresh) {
            statusStore.getSyncOrNull(userWalletId)?.let { return it }
        }
        return try {
            val fresh = fetchStatus(userWalletId)
            statusStore.store(userWalletId, fresh)
            fresh
        } catch (e: Exception) {
            statusStore.getSyncOrNull(userWalletId) ?: throw e
        }
    }

    private suspend fun fetchPromo(userWalletId: UserWalletId): YieldBoostPromo = withContext(dispatchers.io) {
        val response = tangemApi.getPromotions(walletId = userWalletId.stringValue).getOrThrow()
        val dto = response.promotions.firstOrNull { it.name == PROMO_NAME } ?: return@withContext YieldBoostPromo.None
        YieldBoostPromoConverter.convert(dto)
    }

    private suspend fun fetchStatus(userWalletId: UserWalletId): YieldBoostStatus = withContext(dispatchers.io) {
        val response = tangemApi.getYieldBoostStatus(walletId = userWalletId.stringValue).getOrThrow()
        YieldBoostStatusConverter.convert(response)
    }

    private companion object {
        const val PROMO_NAME = "yield-apr-boost"
    }
}