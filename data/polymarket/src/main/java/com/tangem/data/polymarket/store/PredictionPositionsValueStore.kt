package com.tangem.data.polymarket.store

import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PredictionPositionsValue
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PredictionPositionsValueStore @Inject constructor() {

    private val mapStore = RuntimeSharedMapStore<UserWalletId, PredictionPositionsValue.Known>()

    fun get(userWalletId: UserWalletId): Flow<PredictionPositionsValue.Known> = mapStore.get(key = userWalletId)

    suspend fun getSyncOrNull(userWalletId: UserWalletId): PredictionPositionsValue.Known? =
        mapStore.getSyncOrNull(key = userWalletId)

    suspend fun store(userWalletId: UserWalletId, value: PredictionPositionsValue.Known) {
        mapStore.store(key = userWalletId, value = value)
    }

    suspend fun markUnrefreshed(userWalletId: UserWalletId) {
        mapStore.updateIfPresent(key = userWalletId) { it.copy(source = StatusSource.ONLY_CACHE) }
    }

    suspend fun clear(userWalletId: UserWalletId) {
        mapStore.remove(key = userWalletId)
    }
}