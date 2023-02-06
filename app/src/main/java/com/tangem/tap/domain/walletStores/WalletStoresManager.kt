package com.tangem.tap.domain.walletStores

import com.tangem.common.CompletionResult
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.model.WalletStoreModel
import kotlinx.coroutines.flow.Flow

interface WalletStoresManager {
    /**
     * Get all [WalletStoreModel]s updates
     *
     * @return [Flow] with map of [WalletStoreModel] list assigned by [UserWalletId]
     * */
    fun getAll(): Flow<Map<UserWalletId, List<WalletStoreModel>>>

    /**
     * Get [WalletStoreModel]s updates which associated with provided [UserWalletId]
     *
     * @param userWalletId [UserWalletId] of user wallet
     *
     * @return [Flow] with [WalletStoreModel] list
     * */
    fun get(userWalletId: UserWalletId): Flow<List<WalletStoreModel>>
    suspend fun getSync(userWalletId: UserWalletId): List<WalletStoreModel>

    /**
     * Delete [WalletStoreModel]s associated with provided [UserWalletId]s
     *
     * @param userWalletsIds [UserWalletId] list
     *
     * @return [CompletionResult] of operation
     * */
    suspend fun delete(userWalletsIds: List<UserWalletId>): CompletionResult<Unit>

    /**
     * Clear all [WalletStoreModel]s
     *
     * @return [CompletionResult] of operation
     * */
    suspend fun clear(): CompletionResult<Unit>

    /**
     * Fetch wallet stores associated with provided [UserWallet]. Fetched [WalletStoreModel]s updates can be observed
     * with [get] and [getAll] methods
     *
     * @param userWallet [UserWallet] to fetch [WalletStoreModel]s
     * @param refresh If true then recreate already created [WalletStoreModel]s and refresh amounts
     *
     * @return [CompletionResult] of operation
     * */
    suspend fun fetch(
        userWallet: UserWallet,
        refresh: Boolean = false,
    ): CompletionResult<Unit>

    /**
     * Fetch wallet stores associated with provided [UserWallet]s. Fetched [WalletStoreModel]s updates can be observed
     * with [get] and [getAll] methods
     *
     * @param userWallets [UserWallet]s list to fetch [WalletStoreModel]s
     * @param refresh If true then recreate already created [WalletStoreModel]s and refresh amounts
     *
     * @return [CompletionResult] of operation
     * */
    suspend fun fetch(
        userWallets: List<UserWallet>,
        refresh: Boolean = false,
    ): CompletionResult<Unit>

    /**
     * Update [WalletStoreModel]s amounts associated with provided [UserWallet]s
     *
     * @param userWallets [UserWallet]s list to update [WalletStoreModel]s amounts
     *
     * @return [CompletionResult] of operation
     * */
    suspend fun updateAmounts(
        userWallets: List<UserWallet>,
    ): CompletionResult<Unit>

    // For provider
    companion object
}
