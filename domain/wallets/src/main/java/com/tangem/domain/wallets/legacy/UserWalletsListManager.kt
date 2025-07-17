package com.tangem.domain.wallets.legacy

import com.tangem.common.CompletionResult
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

interface UserWalletsListManager {

    /**
     * Indicates that the [UserWalletsListManager] is [UserWalletsListManager.Lockable]
     * */
    val isLockable: Boolean

    /** [Flow] with all saved [UserWallet]s updates */
    val userWallets: Flow<List<UserWallet>>

    /** Count saved wallets updates */
    val savedWalletsCount: Flow<Int>

    /** [Flow] with selected [UserWallet] updates */
    @Deprecated("You should provide the selected wallet via routing parameters due to the scalability of the features")
    val selectedUserWallet: Flow<UserWallet>

    /** [List] with all saved [UserWallet]s updates */
    val userWalletsSync: List<UserWallet>

    /** Selected [UserWallet] */
    @Deprecated("You should provide the selected wallet via routing parameters due to the scalability of the features")
    val selectedUserWalletSync: UserWallet?

    /** Indicates that the [UserWalletsListManager] contains at least one saved [UserWallet] */
    val hasUserWallets: Boolean

    /** Count of saved user wallets */
    val walletsCount: Int

    /**
     * Set [UserWallet] with provided [UserWalletId] as selected
     *
     * @param userWalletId [UserWalletId] of [UserWallet] which must be selected
     *
     * @return [CompletionResult.Success] with selected [UserWallet] or [CompletionResult.Failure] with
     * [NoSuchElementException] if [UserWallet] with [userWalletId] not found
     */
    suspend fun select(userWalletId: UserWalletId): CompletionResult<UserWallet>

    /**
     * Save provided user wallet and set it as selected
     *
     * @param userWallet [UserWallet] to save
     * @param canOverride If false, then terminate with [UserWalletsListError.WalletAlreadySaved] when user tries
     * to save an already saved card
     *
     * @return [CompletionResult] of operation
     */
    suspend fun save(userWallet: UserWallet, canOverride: Boolean = false): CompletionResult<Unit>

    /**
     * Same as [save] but not change selected user wallet ID and not terminate with
     * [UserWalletsListError.WalletAlreadySaved] if [UserWallet] already saved.
     * Can terminate with [NoSuchElementException] if unable to find [UserWallet] with provided [UserWalletId].
     *
     * @param userWalletId update [UserWallet] with that [UserWalletId]
     * @param update lambda that receives stored [UserWallet] and returns updated [UserWallet]
     *
     * @return [CompletionResult.Success] with updated [UserWallet] or [CompletionResult.Failure] with
     * [NoSuchElementException] if [UserWallet] with [userWalletId] not found
     */
    suspend fun update(
        userWalletId: UserWalletId,
        update: suspend (UserWallet) -> UserWallet,
    ): CompletionResult<UserWallet>

    /**
     * Delete saved [UserWallet]s with provided [UserWalletId]s.
     * Sets [isLocked] as true if [userWallets] is empty or if all [userWallets] are locked.
     *
     * @param userWalletIds [UserWalletId]s of [UserWallet]s which must be deleted
     *
     * @return [CompletionResult] of operation
     */
    suspend fun delete(userWalletIds: List<UserWalletId>): CompletionResult<Unit>

    /**
     * Clear all saved [UserWallet]s and set [isLocked] as true
     *
     * @return [CompletionResult] of operation
     */
    suspend fun clear(): CompletionResult<Unit>

    /**
     * Get [UserWallet] with provided [UserWalletId]
     *
     * @return [CompletionResult.Success] with found [UserWallet] or [CompletionResult.Failure] with
     * [NoSuchElementException] if [UserWallet] with [userWalletId] not found
     */
    suspend fun get(userWalletId: UserWalletId): CompletionResult<UserWallet>

    interface Lockable : UserWalletsListManager {

        /**
         * Indicates that all [UserWallet]s is locked
         *
         * @see [isLockedSync]
         * @see [UserWallet.isLocked]
         */
        val isLocked: Flow<Boolean>

        /**
         * Indicates that all [UserWallet]s is locked. Sync version.
         *
         * @see [isLocked]
         * @see [UserWallet.isLocked]
         */
        val isLockedSync: Boolean

        /**
         * Receive saved [UserWallet]s, populate [userWallets] flow with it and set [isLocked] as false.
         *
         * @param type Defines the behavior of the operation.
         *
         * @return [CompletionResult] of operation, with selected [UserWallet]
         * or null if there is no selected [UserWallet]
         */
        suspend fun unlock(type: UnlockType): CompletionResult<UserWallet>

        /** Remove [UserWallet]s from [userWallets] and set [isLocked] as true */
        fun lock()

        /**
         * Defines the behavior of the [unlock] operation.
         * */
        enum class UnlockType {
            /**
             * Ensures that all stored [UserWallet]s are unlocked,
             * or throws [UserWalletsListError.NotAllUserWalletsUnlocked].
             *
             * In this type [selectedUserWallet] is either a previously selected [UserWallet] or the first stored
             * [UserWallet].
             * */
            ALL,

            /**
             * Ensures that at least one stored [UserWallet] is unlocked,
             * or throws [UserWalletsListError.NoUserWalletSelected].
             *
             * In this type [selectedUserWallet] is the first stored and unlocked [UserWallet].
             * */
            ANY,

            /**
             * Same as [ALL] type, but this type can not change [selectedUserWallet] while unlocking.
             * */
            ALL_WITHOUT_SELECT,
        }
    }

    // For provider
    companion object
}