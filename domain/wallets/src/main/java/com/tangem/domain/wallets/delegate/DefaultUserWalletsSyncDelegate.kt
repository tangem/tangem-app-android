package com.tangem.domain.wallets.delegate

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.common.CompletionResult
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UpdateWalletError
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.models.UserWalletRemoteInfo
import com.tangem.domain.wallets.models.copy
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

class DefaultUserWalletsSyncDelegate(
    private val userWalletsListManager: UserWalletsListManager,
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

    // TODO remove dispatchers whnen UserWalletsListManager will be main safe
    private suspend fun renameUserWallet(
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

            return@withContext when (
                val result =
                    userWalletsListManager.update(userWalletId) { it.copy(name = name) }
            ) {
                is CompletionResult.Failure -> raise(UpdateWalletError.DataError(result.error))
                is CompletionResult.Success -> result.data
            }
        }
    }
}