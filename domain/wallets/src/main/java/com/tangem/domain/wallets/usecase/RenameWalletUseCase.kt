package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.common.CompletionResult
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UpdateWalletError
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

/**
 * Use case for rename user wallet
 *
 * @property userWalletsListManager user wallets list manager
 */
class RenameWalletUseCase(
    private val userWalletsListManager: UserWalletsListManager,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, name: String): Either<UpdateWalletError, UserWallet> =
        withContext(dispatchers.io) {
            either {
                val existingNames = userWalletsListManager.userWalletsSync

                ensure(existingNames.none { it.name == name && it.walletId != userWalletId }) {
                    UpdateWalletError.NameAlreadyExists
                }

                when (val result = userWalletsListManager.update(userWalletId) { it.copy(name = name) }) {
                    is CompletionResult.Failure -> raise(UpdateWalletError.DataError(result.error))
                    is CompletionResult.Success -> result.data
                }
            }
        }
}
