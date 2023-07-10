package com.tangem.tap.domain.walletStores.repository

import com.tangem.common.CompletionResult
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.features.wallet.models.Currency
import kotlinx.coroutines.flow.Flow

interface WalletStoresRepository {
    fun getAll(): Flow<Map<UserWalletId, List<WalletStoreModel>>>
    fun get(userWalletId: UserWalletId): Flow<List<WalletStoreModel>>
    suspend fun getSync(userWalletId: UserWalletId): List<WalletStoreModel>

    suspend fun contains(userWalletId: UserWalletId): Boolean

    suspend fun delete(userWalletsIds: List<UserWalletId>): CompletionResult<Unit>

    suspend fun deleteDifference(
        userWalletId: UserWalletId,
        currentBlockchains: List<Currency.Blockchain>,
    ): CompletionResult<Unit>

    suspend fun clear(): CompletionResult<Unit>

    suspend fun storeOrUpdate(userWalletId: UserWalletId, walletStore: WalletStoreModel): CompletionResult<Unit>

    /**
     * Updates [WalletStoreModel] in storage for user wallet with provided [UserWalletId]
     *
     * @param userWalletId [UserWalletId] of user wallet
     * @param operation Lambda which receives list of [WalletStoreModel] assigned to user wallet with [userWalletId]
     * and returns updated [WalletStoreModel]. If null returned then do nothing
     *
     * @return [CompletionResult] of operation
     * */
    suspend fun update(
        userWalletId: UserWalletId,
        operation: (List<WalletStoreModel>) -> WalletStoreModel?,
    ): CompletionResult<Unit>

    companion object
}
