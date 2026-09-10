package com.tangem.data.polymarket.store

import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PredictionDepositRequisitesStore
import com.tangem.domain.polymarket.model.PolymarketDepositRequisites
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

internal class DefaultPredictionDepositRequisitesStore @Inject constructor() : PredictionDepositRequisitesStore {

    private val runtimeStore = RuntimeSharedStore<Map<String, PolymarketDepositRequisites>>()

    override fun get(userWalletId: UserWalletId): Flow<PolymarketDepositRequisites?> = runtimeStore.get()
        .onStart { emit(runtimeStore.getSyncOrDefault(emptyMap())) }
        .map { stored -> stored[userWalletId.stringValue] }
        .distinctUntilChanged()

    override suspend fun getSyncOrNull(userWalletId: UserWalletId): PolymarketDepositRequisites? =
        runtimeStore.getSyncOrNull()?.get(userWalletId.stringValue)

    override suspend fun store(userWalletId: UserWalletId, value: PolymarketDepositRequisites) {
        runtimeStore.update(default = emptyMap()) { stored -> stored + (userWalletId.stringValue to value) }
    }

    override suspend fun clear(userWalletId: UserWalletId) {
        runtimeStore.update(default = emptyMap()) { stored -> stored - userWalletId.stringValue }
    }
}