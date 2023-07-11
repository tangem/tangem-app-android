package com.tangem.tap.domain.userWalletList.implementation

import com.tangem.common.CompletionResult
import com.tangem.common.catching
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.tap.domain.userWalletList.UserWalletsListError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
internal class RuntimeUserWalletsListManager : UserWalletsListManager {
    private val state = MutableStateFlow(State())

    override val userWallets: Flow<List<UserWallet>>
        get() = state
            .mapLatest { listOfNotNull(it.userWallet) }
            .distinctUntilChanged()

    override val selectedUserWallet: Flow<UserWallet>
        get() = state
            .mapLatest { it.userWallet }
            .filterNotNull()
            .distinctUntilChanged()

    override val selectedUserWalletSync: UserWallet?
        get() = state.value.userWallet

    override val hasUserWallets: Boolean
        get() = state.value.userWallet != null

    /**
     * only 1 wallet stored in runtime implementation
     */
    override val walletsCount: Int
        get() = 1

    override suspend fun select(userWalletId: UserWalletId): CompletionResult<UserWallet> = catching {
        state.value.userWallet
            ?.takeIf { it.walletId == userWalletId }
            ?: walletNotFound()
    }

    override suspend fun save(userWallet: UserWallet, canOverride: Boolean): CompletionResult<Unit> {
        return if (canOverride) {
            saveInternal(userWallet)
        } else {
            val isWalletSaved = state.value.userWallet?.walletId == userWallet.walletId

            if (isWalletSaved) {
                CompletionResult.Failure(UserWalletsListError.WalletAlreadySaved)
            } else {
                saveInternal(userWallet)
            }
        }
    }

    override suspend fun update(
        userWalletId: UserWalletId,
        update: suspend (UserWallet) -> UserWallet,
    ): CompletionResult<UserWallet> = catching {
        val wallet = state.value.userWallet
            ?.takeIf { it.walletId == userWalletId }
            ?: walletNotFound()

        state.updateAndGet { prevState ->
            prevState.copy(
                userWallet = update(wallet),
            )
        }.userWallet!!
    }

    override suspend fun delete(userWalletIds: List<UserWalletId>): CompletionResult<Unit> = clear()

    override suspend fun clear(): CompletionResult<Unit> = catching {
        state.update { prevState ->
            prevState.copy(
                userWallet = null,
            )
        }
    }

    override suspend fun get(userWalletId: UserWalletId): CompletionResult<UserWallet> = catching {
        state.value.userWallet ?: walletNotFound()
    }

    private fun saveInternal(userWallet: UserWallet): CompletionResult<Unit> = catching {
        state.update { prevState ->
            prevState.copy(
                userWallet = userWallet,
            )
        }
    }

    private fun walletNotFound(): Nothing {
        throw NoSuchElementException("User wallet not found")
    }

    private data class State(
        val userWallet: UserWallet? = null,
    )
}