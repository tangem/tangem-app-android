package com.tangem.domain.wallets.delegate

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.common.CompletionResult
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UpdateWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.UserWalletRemoteInfo
import com.tangem.domain.models.wallet.copy
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

class DefaultUserWalletsSyncDelegate(
    private val userWalletsListManager: UserWalletsListManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val useNewRepository: Boolean,
    private val dispatchers: CoroutineDispatcherProvider,
) : UserWalletsSyncDelegate {

    override suspend fun syncWallet(userWalletId: UserWalletId, name: String): Either<UpdateWalletError, UserWallet> {
        return renameUserWallet(userWalletId, name)
    }

    override suspend fun syncWallets(list: List<UserWalletRemoteInfo>): Either<UpdateWalletError, Unit> = either {
        list.forEach { userWallet ->
            renameUserWallet(userWallet.walletId, userWallet.name).bind()
        }
    }

    private suspend fun renameUserWallet(
        userWalletId: UserWalletId,
        name: String,
    ): Either<UpdateWalletError, UserWallet> = if (useNewRepository) {
        renameUserWalletInNewRepository(userWalletId, name)
    } else {
        renameUserWalletInLegacyRepository(userWalletId, name)
    }

    private suspend fun renameUserWalletInNewRepository(
        userWalletId: UserWalletId,
        name: String,
    ): Either<UpdateWalletError, UserWallet> = either {
        val userWallets = userWalletsListRepository.userWalletsSync()
        val userWallet = userWallets.find { it.walletId == userWalletId }
            ?: raise(UpdateWalletError.DataError(IllegalStateException("User wallet with id $userWalletId not found")))

        ensure(userWallets.none { it.name == name && it.walletId != userWalletId }) {
            UpdateWalletError.NameAlreadyExists
        }

        ensure(name != userWallet.name) {
            UpdateWalletError.NameAlreadyExists
        }

        val updatedWallet = userWallet.copy(name = name)

        userWalletsListRepository.saveWithoutLock(updatedWallet, canOverride = true)
            .map { updatedWallet }
            .mapLeft { error -> UpdateWalletError.DataError(IllegalStateException("")) }
            .bind()
    }

    // TODO remove dispatchers whnen UserWalletsListManager will be main safe
    private suspend fun renameUserWalletInLegacyRepository(
        userWalletId: UserWalletId,
        name: String,
    ): Either<UpdateWalletError, UserWallet> = withContext(dispatchers.io) {
        either {
            val existingNames = userWalletsListManager.userWalletsSync

            ensure(existingNames.none { it.name == name && it.walletId != userWalletId }) {
                UpdateWalletError.NameAlreadyExists
            }

            val previousName = existingNames.firstOrNull { it.walletId == userWalletId }?.name.orEmpty()
            if (previousName == name) {
                raise(UpdateWalletError.NameAlreadyExists)
            }

            when (
                val result =
                    userWalletsListManager.update(userWalletId) { it.copy(name = name) }
            ) {
                is CompletionResult.Failure -> raise(UpdateWalletError.DataError(result.error))
                is CompletionResult.Success -> result.data
            }
        }
    }
}