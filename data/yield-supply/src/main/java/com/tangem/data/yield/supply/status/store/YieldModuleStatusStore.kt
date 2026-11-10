package com.tangem.data.yield.supply.status.store

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus
import kotlinx.coroutines.flow.Flow

internal interface YieldModuleStatusStore {

    fun get(userWalletId: UserWalletId): Flow<Set<YieldModuleStatus>>

    suspend fun getSync(userWalletId: UserWalletId): Set<YieldModuleStatus>

    suspend fun refresh(userWalletId: UserWalletId, keys: Set<YieldModuleStatus.Key>)

    /** Stores [statuses] as [com.tangem.domain.models.StatusSource.ACTUAL], whatever their source is */
    suspend fun storeActual(userWalletId: UserWalletId, statuses: Set<YieldModuleStatus>)

    suspend fun storeError(userWalletId: UserWalletId, keys: Set<YieldModuleStatus.Key>)

    suspend fun clear(userWalletId: UserWalletId, keys: Set<YieldModuleStatus.Key>)

    suspend fun remove(userWalletIds: List<UserWalletId>)
}