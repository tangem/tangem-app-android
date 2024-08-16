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
    private val implementation: MutableStateFlow<UserWalletsListManager?> = MutableStateFlow(value = null)

    private val requireImplementation: UserWalletsListManager
        get() = requireNotNull(implementation.value) {
            "UserWalletsListManager is not initialized"
        }

    init {
        subscribeOnCurrentManager()
    }

    override val isLockable: Boolean
        get() = requireImplementation.isLockable

    override val userWallets: Flow<List<UserWallet>>
        get() = implementation
            .transformLatest { impl ->
                if (impl != null) {
                    emitAll(impl.userWallets)
                }
            }
            // To avoid returning empty flow to subscriber while implementation and userWallets are null
            // Flow is called first time when implementation is null and then when its assigned with implementation
            // that may have not user wallets (null or empty).
            // As a result subscription occurs on empty flow, than will not change if user wallets are available
            .filter { requireImplementation.hasUserWallets }

    override val userWalletsSync: List<UserWallet>
        get() = requireImplementation.userWalletsSync

    override val selectedUserWallet: Flow<UserWallet>
        get() = implementation
            .transformLatest { impl ->
                if (impl != null) {
                    emitAll(impl.selectedUserWallet)
                }
            }
            // To avoid returning empty flow to subscriber while implementation and userWallets are null
            // Flow is called first time when implementation is null and then when its assigned with implementation
            // that may have not user wallets (null or empty).
            // As a result subscription occurs on empty flow, than will not change if user wallets are available
            .filter { requireImplementation.hasUserWallets }

    override val selectedUserWalletSync: UserWallet?
        get() = requireImplementation.selectedUserWalletSync

    override val hasUserWallets: Boolean
        get() = requireImplementation.hasUserWallets

    override val walletsCount: Int
        get() = requireImplementation.walletsCount

    override val isLocked: Flow<Boolean>
        get() = implementation.transformLatest { impl ->
            if (impl == null) return@transformLatest

            if (impl is UserWalletsListManager.Lockable) {
                emitAll(impl.isLocked)
            } else {
                error("RuntimeUserWalletsListManager is not lockable")
            }
        }

    override val isLockedSync: Boolean
        get() {
            val impl = requireImplementation

            return if (impl is UserWalletsListManager.Lockable) {
                impl.isLockedSync
            } else {
                error("RuntimeUserWalletsListManager is not lockable")
            }
        }

    override suspend fun select(userWalletId: UserWalletId): CompletionResult<UserWallet> {
        return requireImplementation.select(userWalletId)
    }

    override suspend fun save(userWallet: UserWallet, canOverride: Boolean): CompletionResult<Unit> {
        return requireImplementation.save(userWallet, canOverride)
    }

    override suspend fun update(
        userWalletId: UserWalletId,
        update: suspend (UserWallet) -> UserWallet,
    ): CompletionResult<UserWallet> {
        return requireImplementation.update(userWalletId, update)
    }

    override suspend fun delete(userWalletIds: List<UserWalletId>): CompletionResult<Unit> {
        return requireImplementation.delete(userWalletIds)
    }

    override suspend fun clear(): CompletionResult<Unit> {
        return requireImplementation.clear()
    }

    override suspend fun get(userWalletId: UserWalletId): CompletionResult<UserWallet> {
        return requireImplementation.get(userWalletId)
    }

    override suspend fun unlock(type: UserWalletsListManager.Lockable.UnlockType): CompletionResult<UserWallet> {
        val implementation = requireImplementation

        return if (implementation is UserWalletsListManager.Lockable) {
            implementation.unlock(type)
        } else {
            error("RuntimeUserWalletsListManager is not lockable")
        }
    }

    override fun lock() {
        val implementation = requireImplementation

        return if (implementation is UserWalletsListManager.Lockable) {
            implementation.lock()
        } else {
            error("RuntimeUserWalletsListManager is not lockable")
        }
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
                    Timber.e("Switch to the same manager ${possibleManager::class.simpleName}")
                }

                Timber.i("Switch to ${possibleManager::class.simpleName}")

                val previousManager = implementation.value
                implementation.value = copySelectedUserWallet(
                    sourceManager = previousManager,
                    destinationManager = possibleManager,
                )

                previousManager?.clear()
            }
            .flowOn(dispatchers.io)
            .launchIn(applicationScope)
    }

    private suspend fun copySelectedUserWallet(
        sourceManager: UserWalletsListManager?,
        destinationManager: UserWalletsListManager,
    ): UserWalletsListManager {
        sourceManager?.selectedUserWalletSync?.let { selectedWallet ->
            destinationManager.save(selectedWallet, canOverride = true)
        }

        return destinationManager
    }
}