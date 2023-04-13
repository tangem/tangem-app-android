package com.tangem.tap.domain.userWalletList

import com.tangem.common.CompletionResult
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.model.UserWallet
import kotlinx.coroutines.flow.Flow

interface UserWalletsListManager {
    /**
     * [Flow] with all saved [UserWallet]s updates
     * */
    val userWallets: Flow<List<UserWallet>>

    /**
     * [Flow] with selected [UserWallet] updates
     * */
    val selectedUserWallet: Flow<UserWallet>

    /**
     * Selected [UserWallet]
     * */
    val selectedUserWalletSync: UserWallet?

    /**
     * Indicates that the [UserWalletsListManager] contains at least one saved [UserWallet]
     * */
    val hasUserWallets: Boolean

    /**
     * Set [UserWallet] with provided [UserWalletId] as selected
     *
     * @param userWalletId [UserWalletId] of [UserWallet] which must be selected
     *
     * @return [CompletionResult.Success] with selected [UserWallet]
     * or [CompletionResult.Failure] with [NoSuchElementException] if [UserWallet] with [userWalletId] not found
     * */
    suspend fun select(userWalletId: UserWalletId): CompletionResult<UserWallet>

    /**
     * Save provided user wallet and set it as selected
     * @param userWallet [UserWallet] to save
     * @param canOverride If false, then terminate with [UserWalletsListError.WalletAlreadySaved] when user tries
     * to save an already saved card
     * @return [CompletionResult] of operation
     */
    suspend fun save(userWallet: UserWallet, canOverride: Boolean = false): CompletionResult<Unit>

    /**
     * Same as [save] but not change selected user wallet ID
     * and not terminate with [UserWalletsListError.WalletAlreadySaved] if [UserWallet] already saved
     *
     * Can terminate with [NoSuchElementException] if unable to find [UserWallet] with provided [UserWalletId]
     * @param userWalletId update [UserWallet] with that [UserWalletId]
     * @param update lambda that receives stored [UserWallet] and returns updated [UserWallet]
     * @return [CompletionResult.Success] with updated [UserWallet]
     * or [CompletionResult.Failure] with [NoSuchElementException] if [UserWallet] with [userWalletId] not found
     * */
    suspend fun update(
        userWalletId: UserWalletId,
        update: suspend (UserWallet) -> UserWallet,
    ): CompletionResult<UserWallet>

    /**
     * Delete saved [UserWallet]s with provided [UserWalletId]s
     *
     * Sets [isLocked] as true if [userWallets] is empty or if all [userWallets] are locked
     *
     * @param userWalletIds [UserWalletId]s of [UserWallet]s which must be deleted
     *
     * @return [CompletionResult] of operation
     * */
    suspend fun delete(userWalletIds: List<UserWalletId>): CompletionResult<Unit>

    /**
     * Clear all saved [UserWallet]s and set [isLocked] as true
     *
     * @return [CompletionResult] of operation
     * */
    suspend fun clear(): CompletionResult<Unit>

    /**
     * Get [UserWallet] with provided [UserWalletId]
     *
     * @return [CompletionResult.Success] with found [UserWallet]
     * or [CompletionResult.Failure] with [NoSuchElementException] if [UserWallet] with [userWalletId] not found
     * */
    suspend fun get(userWalletId: UserWalletId): CompletionResult<UserWallet>

    interface Lockable : UserWalletsListManager {
        /**
         * Indicates that all [UserWallet]s is locked
         *
         * @see [isLockedSync]
         * @see [UserWallet.isLocked]
         * */
        val isLocked: Flow<Boolean>

        /**
         * Indicates that all [UserWallet]s is locked
         *
         * Sync version
         *
         * @see [isLocked]
         * @see [UserWallet.isLocked]
         * */
        val isLockedSync: Boolean

        /**
         * Receive saved [UserWallet]s, populate [userWallets] flow with it and set [isLocked] as false.
         *
         * @return [CompletionResult] of operation, with selected [UserWallet]
         * or null if there is no selected [UserWallet]
         * */
        suspend fun unlock(): CompletionResult<UserWallet>

        /**
         * Remove [UserWallet]s from [userWallets] and set [isLocked] as true
         * */
        fun lock()
    }

    // For provider
    companion object
}
