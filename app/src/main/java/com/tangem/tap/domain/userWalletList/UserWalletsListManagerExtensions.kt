package com.tangem.tap.domain.userWalletList

import com.tangem.common.CompletionResult
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UserWallet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Indicates that the [UserWalletsListManager] implements [UserWalletsListManager.Lockable]
 * */
val UserWalletsListManager.isLockable: Boolean
    get() = this is UserWalletsListManager.Lockable

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
    get() = asLockable()?.isLockedSync ?: false

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
suspend fun UserWalletsListManager.unlockIfLockable(): CompletionResult<UserWallet> {
    return asLockable()?.unlock() ?: CompletionResult.Failure(UserWalletsListError.UnableToUnlockUserWallets())
}

/**
 * Call [UserWalletsListManager.Lockable.lock] if [UserWalletsListManager] implements [UserWalletsListManager.Lockable]
 * or do nothing otherwise
 *
 * @see UserWalletsListManager.Lockable.lock
 * */
fun UserWalletsListManager.lockIfLockable() {
    asLockable()?.lock()
}

/**
 * Safe cast [UserWalletsListManager] to [UserWalletsListManager.Lockable]
 *
 * @return If [UserWalletsListManager] not implements [UserWalletsListManager.Lockable] then returns null or
 * [UserWalletsListManager.Lockable] otherwise
 * */
fun UserWalletsListManager.asLockable(): UserWalletsListManager.Lockable? {
    return this as? UserWalletsListManager.Lockable
}