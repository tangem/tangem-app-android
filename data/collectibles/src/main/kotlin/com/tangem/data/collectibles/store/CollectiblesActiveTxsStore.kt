package com.tangem.data.collectibles.store

import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.domain.collectibles.models.CollectibleActiveTx
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

internal class CollectiblesActiveTxsStore(
    private val store: RuntimeSharedStore<Map<UserWalletId, List<CollectibleActiveTx>>>,
) {

    fun get(userWalletId: UserWalletId): Flow<List<CollectibleActiveTx>> = store.get().mapNotNull { it[userWalletId] }

    suspend fun store(userWalletId: UserWalletId, activeTxs: List<CollectibleActiveTx>) {
        store.update(default = emptyMap()) { stored -> stored + (userWalletId to activeTxs) }
    }
}