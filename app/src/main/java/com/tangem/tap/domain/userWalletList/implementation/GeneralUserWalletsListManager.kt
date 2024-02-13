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
 * @author Andrew Khokhlov on 09/02/2024
 */
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
        get() = channelFlow {
            implementation.collectLatest {
                it.userWallets.collectLatest(::send)
            }
        }

    override val selectedUserWallet: Flow<UserWallet>
        get() = channelFlow {
            implementation.collectLatest {
                it.selectedUserWallet.collectLatest(::send)
            }
        }

    override val selectedUserWalletSync: UserWallet?
        get() = implementation.value.selectedUserWalletSync

    override val hasUserWallets: Boolean
        get() = implementation.value.hasUserWallets

    override val walletsCount: Int
        get() = implementation.value.walletsCount

    override val isLocked: Flow<Boolean>
        get() = channelFlow {
            implementation.collectLatest {
                if (it is UserWalletsListManager.Lockable) {
                    it.isLocked.collectLatest(::send)
                } else {
                    error("RuntimeUserWalletsListManager is not lockable")
                }
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

    override suspend fun unlock(throwIfNotAllWalletsUnlocked: Boolean): CompletionResult<UserWallet> {
        val implementation = implementation.value
        return if (implementation is UserWalletsListManager.Lockable) {
            implementation.unlock()
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

    private fun subscribeOnCurrentManager() {
        appPreferencesStore.get(key = PreferencesKeys.SAVE_USER_WALLETS_KEY, default = false)
            .distinctUntilChanged()
            .map { shouldSaveUserWallets ->
                if (shouldSaveUserWallets) {
                    biometricUserWalletsListManager.copyFrom(runtimeUserWalletsListManager)
                } else {
                    runtimeUserWalletsListManager.copyFrom(biometricUserWalletsListManager)
                }
            }
            .onEach {
                Timber.d("Switch to ${it::class.java.simpleName}")

                implementation.value = it
            }
            .flowOn(dispatchers.main)
            .launchIn(applicationScope)
    }

    /** Copy data from [old] manager and clean it */
    private suspend fun UserWalletsListManager.copyFrom(old: UserWalletsListManager): UserWalletsListManager {
        old.selectedUserWalletSync?.let { this.save(it) }
        old.clear()

        return this
    }
}
