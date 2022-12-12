package com.tangem.tap.domain.userWalletList.implementation

import com.tangem.common.CompletionResult
import com.tangem.common.catching
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.userWalletList.UserWalletsListManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class DummyUserWalletsListManager : UserWalletsListManager {
    override val userWallets: Flow<List<UserWallet>>
        get() = flowOf(emptyList())
    override val selectedUserWallet: Flow<UserWallet>
        get() = flowOf()
    override val selectedUserWalletSync: UserWallet?
        get() = null
    override val isLocked: Flow<Boolean>
        get() = flowOf(true)
    override val isLockedSync: Boolean
        get() = true
    override val hasSavedUserWallets: Boolean
        get() = false

    override suspend fun unlockWithBiometry(): CompletionResult<UserWallet?> {
        return CompletionResult.Success(null)
    }

    override suspend fun unlockWithCard(userWallet: UserWallet): CompletionResult<Unit> {
        return CompletionResult.Success(Unit)
    }

    override fun lock() {
        /* no-op */
    }

    override suspend fun selectWallet(walletId: UserWalletId): CompletionResult<UserWallet> {
        return catching {
            error("Not implemented")
        }
    }

    override suspend fun save(userWallet: UserWallet, canOverride: Boolean): CompletionResult<Unit> {
        return CompletionResult.Success(Unit)
    }

    override suspend fun delete(walletIds: List<UserWalletId>): CompletionResult<Unit> {
        return CompletionResult.Success(Unit)
    }

    override suspend fun clear(): CompletionResult<Unit> {
        return CompletionResult.Success(Unit)
    }

    override suspend fun get(walletId: UserWalletId): CompletionResult<UserWallet> {
        return catching {
            error("Not implemented")
        }
    }
}
