package com.tangem.domain.core.wallets

import arrow.core.Either
import com.tangem.domain.core.wallets.error.DeleteWalletError
import com.tangem.domain.core.wallets.error.LockWalletsError
import com.tangem.domain.core.wallets.error.SaveWalletError
import com.tangem.domain.core.wallets.error.SelectWalletError
import com.tangem.domain.core.wallets.error.SetLockError
import com.tangem.domain.core.wallets.error.UnlockWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository for managing user wallets list.
 * It provides methods to load, select, save, lock, unlock, and delete user wallets.
 *
 * TODO tests [REDACTED_TASK_KEY]
 *
 * @see com.tangem.domain.models.wallet.UserWallet
 * @see com.tangem.domain.models.wallet.UserWalletId
 */
interface UserWalletsListRepository {

    /**
     * List of user wallets.
     * It can be null if the list is not loaded yet.
     */
    val userWallets: StateFlow<List<UserWallet>?>

    /**
     * Currently selected user wallet.
     * It can be null if wallets list is not loaded yet or wallets list is empty.
     */
    val selectedUserWallet: StateFlow<UserWallet?>

    /**
     * Loads user wallets list and selected wallet.
     * If the list is already loaded, it does nothing.
     */
    suspend fun load()

    /**
     * Gets and if necessary loads user wallets list and selected wallet.
     */
    suspend fun userWalletsSync(): List<UserWallet>

    /**
     * Gets and if necessary loads selected user wallet.
     */
    suspend fun selectedUserWalletSync(): UserWallet?

    /**
     * Selects user wallet by id.
     * If the wallet is not found, it returns [SelectWalletError.UnableToSelectUserWallet].
     */
    suspend fun select(userWalletId: UserWalletId): Either<SelectWalletError, UserWallet>

    /**
     * Saves user wallet.
     * If the wallet already exists and [canOverride] is false, it returns [SaveWalletError.WalletAlreadySaved].
     * If the wallet already exists and [canOverride] is true, it overrides the existing wallet.
     *
     * Does not lock the wallet after saving, it should be done manually using [setLock] method.
     */
    suspend fun saveWithoutLock(
        userWallet: UserWallet,
        canOverride: Boolean = true,
    ): Either<SaveWalletError, UserWallet>

    /**
     * Sets lock for **unlocked** user wallet.
     * If the wallet is not found, it returns [SetLockError.UserWalletNotFound]
     * If the wallet is locked, it returns [SetLockError.UserWalletLocked]
     * If the lock method is not supported, it returns [SetLockError.UnableToSetLock].
     */
    suspend fun setLock(userWalletId: UserWalletId, lockMethod: LockMethod): Either<SetLockError, Unit>

    /**
     * Deletes user wallets by ids.
     * If the wallet is not found, it returns [DeleteWalletError.UnableToDelete]
     */
    suspend fun delete(userWalletIds: List<UserWalletId>): Either<DeleteWalletError, Unit>

    /**
     * Unlocks specific user wallet.
     * If the wallet is already unlocked, returns [UnlockWalletError.AlreadyUnlocked].
     * If the wallet is not found, returns [UnlockWalletError.UserWalletNotFound].
     * If the unlock method is not supported, returns [UnlockWalletError.UnableToUnlock]
     * If the user cancels the unlock operation (ex. dismisses dialogs), returns [UnlockWalletError.UserCancelled].
     * If the scanned card does not match the wallet, returns [UnlockWalletError.ScannedCardWalletNotMatched].
     */
    suspend fun unlock(userWalletId: UserWalletId, unlockMethod: UnlockMethod): Either<UnlockWalletError, Unit>

    /**
     * Unlocks all user wallets using biometric authentication.
     * If all the wallets was are already unlocked, returns [UnlockWalletError.AlreadyUnlocked].
     * Success if at least one wallet was unlocked.
     * If the biometric method is not supported for some of user wallets, returns [UnlockWalletError.UnableToUnlock]
     */
    suspend fun unlockAllWallets(): Either<UnlockWalletError, Unit>

    /**
     * Locks all secured user wallets (wallets that are not locked with [LockMethod.NoLock]).
     * If all the wallets are already locked or unsecured, returns [LockWalletsError.NothingToLock].
     * Success if at least one wallet was locked.
     */
    suspend fun lockAllWallets(): Either<LockWalletsError, Unit>

    /**
     * Clears all persistent data related to user wallets.
     * This includes removing all user wallets, selected wallet, and any other related data.
     * User wallets will stay in the cache, but will be reloaded on next repository initialization.
     */
    suspend fun clearPersistentData()

    sealed class LockMethod {
        data object Biometric : LockMethod()
        class AccessCode(val accessCode: CharArray) : LockMethod()
        data object NoLock : LockMethod()
    }

    enum class UnlockMethod {
        Biometric,
        AccessCode,
        Scan,
    }
}

fun UserWalletsListRepository.requireUserWalletsSync(): List<UserWallet> {
    return userWallets.value ?: error("User wallets list is not loaded")
}