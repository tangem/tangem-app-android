package com.tangem.domain.wallets.legacy

import com.tangem.common.CompletionResult
import com.tangem.domain.wallets.legacy.UserWalletsListManager.Lockable.UnlockType
import com.tangem.domain.models.wallet.UserWallet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Indicates that the [UserWalletsListManager] is locked
 *
 * @return If [UserWalletsListManager] not implements [UserWalletsListManager.Lockable] returns [Flow] which
 * produces only one false value
 *
 * @see UserWalletsListManager.Lockable.isLockedSync
 * */
val UserWalletsListManager.isLocked: Flow<Boolean>
    get() = asLockable()?.isLocked ?: flowOf(false)

/**
 * Indicates that the [UserWalletsListManager] is locked
 *
 * @return If [UserWalletsListManager] not implements [UserWalletsListManager.Lockable] returns false
 *
 * @see UserWalletsListManager.Lockable.isLockedSync
 * */
val UserWalletsListManager.isLockedSync: Boolean
    get() = asLockable()?.isLockedSync == true

/**
 * Call [UserWalletsListManager.Lockable.unlock] if [UserWalletsListManager] implements [UserWalletsListManager.Lockable]
 *
 * @return If [UserWalletsListManager] not implements [UserWalletsListManager.Lockable]
 * returns [CompletionResult.Failure] with [UserWalletsListError.UnableToUnlockUserWallets]
 *
 * If [UserWalletsListManager] implements [UserWalletsListManager.Lockable]
 * returns [CompletionResult.Success] with selected [UserWallet]
 *
 * @see UserWalletsListManager.Lockable.unlock
 * */
suspend fun UserWalletsListManager.unlockIfLockable(type: UnlockType = UnlockType.ANY): CompletionResult<UserWallet> {
    return asLockable()?.unlock(type) ?: CompletionResult.Failure(UserWalletsListError.UnableToUnlockUserWallets())
}

/**
 * Safe cast [UserWalletsListManager] to [UserWalletsListManager.Lockable]
 *
 * @return If [UserWalletsListManager] not implements [UserWalletsListManager.Lockable] then returns null or
 * [UserWalletsListManager.Lockable] otherwise
 * */
fun UserWalletsListManager.asLockable(): UserWalletsListManager.Lockable? {
    if (this.isLockable) {
        return this as? UserWalletsListManager.Lockable
    }
    return null
}