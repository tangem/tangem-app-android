package com.tangem.tap.data

import com.tangem.common.CompletionResult
import com.tangem.common.catching
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class UserWalletsStoreRepositoryProxy(
    private val userWalletsListRepository: UserWalletsListRepository,
) : UserWalletsStore {

    override val selectedUserWalletOrNull: UserWallet?
        get() = userWalletsListRepository.selectedUserWallet.value

    override val userWallets: Flow<List<UserWallet>>
        get() = flow {
            userWalletsListRepository.load()
            userWalletsListRepository.userWallets.collect {
                emit(requireNotNull(it))
            }
        }

    override val userWalletsSync: List<UserWallet>
        get() = userWalletsListRepository.userWallets.value.orEmpty()

    override fun getSyncOrNull(key: UserWalletId): UserWallet? {
        return userWalletsListRepository.userWallets.value?.find { it.walletId == key }
    }

    override fun getSyncStrict(key: UserWalletId): UserWallet {
        return requireNotNull(getSyncOrNull(key)) { "Unable to find user wallet with provided ID: $key" }
    }

    override suspend fun update(
        userWalletId: UserWalletId,
        update: suspend (UserWallet) -> UserWallet,
    ): CompletionResult<UserWallet> {
        return catching {
            val userWallet = userWalletsListRepository.userWallets.value?.find { it.walletId == userWalletId }
            requireNotNull(userWallet) { "Unable to find user wallet with provided ID: $userWalletId" }
            val updatedUserWallet = update(userWallet)
            userWalletsListRepository.saveWithoutLock(
                userWallet = updatedUserWallet,
                canOverride = true,
            )
            updatedUserWallet
        }
    }
}