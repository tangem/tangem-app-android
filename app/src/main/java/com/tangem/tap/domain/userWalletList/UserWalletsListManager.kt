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
     * Indicates that all [UserWallet]s is unlocked
     *
     * @see [isLockedSync]
     * @see [UserWallet.isLocked]
     * */
    val isLocked: Flow<Boolean>

    /**
     * Indicates that all [UserWallet]s is unlocked
     *
     * Sync version
     *
     * @see [isLocked]
     * @see [UserWallet.isLocked]
     * */
    val isLockedSync: Boolean

    val hasSavedUserWallets: Boolean

    /**
     * Receive saved [UserWallet]s, populate [userWallets] flow with it and set [isLocked] as false.
     * Require biometric user authorization
     *
     * @return [CompletionResult] of operation, with selected [UserWallet]
     * or null if there is no selected [UserWallet]
     * */
    suspend fun unlockWithBiometry(): CompletionResult<UserWallet?>

    /**
     * Remove [UserWallet]s from [userWallets] and set [isLocked] as true
     * */
    fun lock()

    /**
     * Set [UserWallet] with provided [UserWalletId] as selected
     *
     * @param userWalletId [UserWalletId] of [UserWallet] which must be selected
     *
     * @return [CompletionResult] of operation with selected [UserWallet]
     * */
    suspend fun selectWallet(userWalletId: UserWalletId): CompletionResult<UserWallet>

    /**
     * Save provided user wallet and set it as selected
     * @param userWallet [UserWallet] to save
     * @param canOverride If false, then terminate with [UserWalletListError.WalletAlreadySaved] when user tries
     * to save an already saved card
     * @return [CompletionResult] of operation
     */
    suspend fun save(userWallet: UserWallet, canOverride: Boolean = false): CompletionResult<Unit>

    /**
     * Same as [save] but not change selected user wallet ID
     * and not terminate with [UserWalletListError.WalletAlreadySaved] if [UserWallet] already saved
     *
     * Can terminate with [NoSuchElementException] if unable to find [UserWallet] with provided [UserWalletId]
     * @param userWalletId update [UserWallet] with that [UserWalletId]
     * @param update lambda that receives stored [UserWallet] and returns updated [UserWallet]
     * @return [CompletionResult] of operation with updated [UserWallet]
     * */
    suspend fun update(userWalletId: UserWalletId, update: (UserWallet) -> UserWallet): CompletionResult<UserWallet>

    /**
     * Delete saved [UserWallet]s with provided [UserWalletId]s
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
     * May terminate with [NoSuchElementException] if [UserWallet] is not found
     *
     * @return [CompletionResult] of operation with found [UserWallet]
     * */
    suspend fun get(userWalletId: UserWalletId): CompletionResult<UserWallet>

    // For provider
    companion object
}
