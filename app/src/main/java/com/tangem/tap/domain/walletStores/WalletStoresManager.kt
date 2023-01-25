package com.tangem.tap.domain.walletStores

import com.tangem.common.CompletionResult
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.model.WalletStoreModel
import kotlinx.coroutines.flow.Flow

interface WalletStoresManager {
    fun getAll(): Flow<Map<UserWalletId, List<WalletStoreModel>>>
    fun get(userWalletId: UserWalletId): Flow<List<WalletStoreModel>>
    suspend fun getSync(userWalletId: UserWalletId): List<WalletStoreModel>

    suspend fun delete(userWalletsIds: List<UserWalletId>): CompletionResult<Unit>
    suspend fun clear(): CompletionResult<Unit>

    suspend fun fetch(
        userWallet: UserWallet,
        refresh: Boolean = false,
    ): CompletionResult<Unit>

    suspend fun fetch(
        userWallets: List<UserWallet>,
        refresh: Boolean = false,
    ): CompletionResult<Unit>

    suspend fun updateAmounts(
        userWallets: List<UserWallet>,
    ): CompletionResult<Unit>

    companion object
}