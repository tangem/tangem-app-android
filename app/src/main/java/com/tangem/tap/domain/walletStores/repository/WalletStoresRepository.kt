package com.tangem.tap.domain.walletStores.repository

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.CompletionResult
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.model.WalletStoreModel
import kotlinx.coroutines.flow.Flow

interface WalletStoresRepository {
    fun getAll(): Flow<Map<UserWalletId, List<WalletStoreModel>>>
    fun get(userWalletId: UserWalletId): Flow<List<WalletStoreModel>>
    suspend fun getSync(userWalletId: UserWalletId): List<WalletStoreModel>

    suspend fun contains(userWalletId: UserWalletId): Boolean

    suspend fun delete(userWalletsIds: List<UserWalletId>): CompletionResult<Unit>

    suspend fun deleteDifference(
        userWalletId: UserWalletId,
        currentBlockchains: List<Blockchain>,
    ): CompletionResult<Unit>

    suspend fun clear(): CompletionResult<Unit>

    suspend fun storeOrUpdate(
        userWalletId: UserWalletId,
        walletStore: WalletStoreModel,
    ): CompletionResult<Unit>

    companion object
}
