package com.tangem.tap.domain.userWalletList.implementation

import com.tangem.common.CompletionResult
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import timber.log.Timber

/**
 * General implementation of [UserWalletsListManager] that helps to switch between Runtime and Biometric
 * implementations.
 *
 * @property runtimeUserWalletsListManager   runtime user wallets list manager
 * @property biometricUserWalletsListManager biometric user wallets list manager
 * @property appPreferencesStore             app preferences store
 * @property dispatchers                     coroutine dispatcher provider
 *
[REDACTED_AUTHOR]
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class GeneralUserWalletsListManager(
    private val runtimeUserWalletsListManager: UserWalletsListManager,
    private val biometricUserWalletsListManager: UserWalletsListManager,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : UserWalletsListManager.Lockable {

    private val applicationScope = CoroutineScope(dispatchers.io)
    private val implementation = MutableStateFlow(runtimeUserWalletsListManager)

    init {
        subscribeOnCurrentManager()
    }

    override val userWallets: Flow<List<UserWallet>>
        get() = implementation.flatMapLatest { it.userWallets }

    override val selectedUserWallet: Flow<UserWallet>
        get() = implementation.flatMapLatest { it.selectedUserWallet }

    override val selectedUserWalletSync: UserWallet?
        get() = implementation.value.selectedUserWalletSync

    override val hasUserWallets: Boolean
        get() = implementation.value.hasUserWallets

    override val walletsCount: Int
        get() = implementation.value.walletsCount

    override val isLocked: Flow<Boolean>
        get() = implementation.flatMapLatest {
            if (it is UserWalletsListManager.Lockable) {
                it.isLocked
            } else {
                error("RuntimeUserWalletsListManager is not lockable")
            }
        }

    override val isLockedSync: Boolean
        get() {
            val implementation = implementation.value
            return if (implementation is UserWalletsListManager.Lockable) {
                implementation.isLockedSync
            } else {
                error("RuntimeUserWalletsListManager is not lockable")
            }
        }

    override suspend fun select(userWalletId: UserWalletId): CompletionResult<UserWallet> {
        return implementation.value.select(userWalletId)
    }

    override suspend fun save(userWallet: UserWallet, canOverride: Boolean): CompletionResult<Unit> {
        return implementation.value.save(userWallet, canOverride)
    }

    override suspend fun update(
        userWalletId: UserWalletId,
        update: suspend (UserWallet) -> UserWallet,
    ): CompletionResult<UserWallet> {
        return implementation.value.update(userWalletId, update)
    }

    override suspend fun delete(userWalletIds: List<UserWalletId>): CompletionResult<Unit> {
        return implementation.value.delete(userWalletIds)
    }

    override suspend fun clear(): CompletionResult<Unit> {
        return implementation.value.clear()
    }

    override suspend fun get(userWalletId: UserWalletId): CompletionResult<UserWallet> {
        return implementation.value.get(userWalletId)
    }

    override suspend fun unlock(type: UserWalletsListManager.Lockable.UnlockType): CompletionResult<UserWallet> {
        val implementation = implementation.value
        return if (implementation is UserWalletsListManager.Lockable) {
            implementation.unlock(type)
        } else {
            error("RuntimeUserWalletsListManager is not lockable")
        }
    }

    override fun lock() {
        val implementation = implementation.value
        return if (implementation is UserWalletsListManager.Lockable) {
            implementation.lock()
        } else {
            error("RuntimeUserWalletsListManager is not lockable")
        }
    }

    override fun isLockable(): Boolean {
        return implementation.value.isLockable()
    }

    private fun subscribeOnCurrentManager() {
        appPreferencesStore.get(key = PreferencesKeys.SAVE_USER_WALLETS_KEY, default = false)
            .distinctUntilChanged()
            .onEach { shouldSaveUserWallets ->
                val possibleManager = if (shouldSaveUserWallets) {
                    biometricUserWalletsListManager
                } else {
                    runtimeUserWalletsListManager
                }

                if (possibleManager == implementation.value) {
                    Timber.e("Switch to same manager ${possibleManager::class.simpleName}")
                }

                Timber.d("Switch to ${possibleManager::class.simpleName}")

                val previousManager = implementation.value
                implementation.value = copySelectedUserWallet(
                    sourceManager = previousManager,
                    destinationManager = possibleManager,
                )

                previousManager.clear()
            }
            .flowOn(dispatchers.io)
            .launchIn(applicationScope)
    }

    private suspend fun copySelectedUserWallet(
        sourceManager: UserWalletsListManager,
        destinationManager: UserWalletsListManager,
    ): UserWalletsListManager {
        sourceManager.selectedUserWalletSync?.let { selectedWallet ->
            destinationManager.save(selectedWallet, canOverride = true)
        }

        return destinationManager
    }
}