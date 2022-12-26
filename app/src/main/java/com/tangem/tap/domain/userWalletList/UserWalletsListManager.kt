package com.tangem.tap.domain.userWalletList

import com.tangem.common.CompletionResult
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.model.UserWallet
import kotlinx.coroutines.flow.Flow

interface UserWalletsListManager {
    val userWallets: Flow<List<UserWallet>>
    val selectedUserWallet: Flow<UserWallet>
    val selectedUserWalletSync: UserWallet?
    val isLocked: Flow<Boolean>
    val isLockedSync: Boolean
    val hasSavedUserWallets: Boolean

    suspend fun unlockWithBiometry(): CompletionResult<UserWallet?>
    fun lock()

    suspend fun selectWallet(userWalletId: UserWalletId): CompletionResult<UserWallet>

    /**
     * Save user wallet
     * @param userWallet [UserWallet] to save
     * @param canOverride If false, then terminate with [UserWalletListError.WalletAlreadySaved] when user tries to save an
     * already saved card
     * @return [CompletionResult] of operation
     * */
    suspend fun save(userWallet: UserWallet, canOverride: Boolean = false): CompletionResult<Unit>

    suspend fun delete(userWalletIds: List<UserWalletId>): CompletionResult<Unit>
    suspend fun clear(): CompletionResult<Unit>

    suspend fun get(userWalletId: UserWalletId): CompletionResult<UserWallet>

    companion object
}
