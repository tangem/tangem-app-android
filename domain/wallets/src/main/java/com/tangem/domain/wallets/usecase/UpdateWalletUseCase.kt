package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.common.CompletionResult
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UpdateWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Use case for updating user wallet
 *
 * @property userWalletsListManager user wallets list manager
 *
[REDACTED_AUTHOR]
 */
class UpdateWalletUseCase(private val userWalletsListManager: UserWalletsListManager) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        update: suspend (UserWallet) -> UserWallet,
    ): Either<UpdateWalletError, UserWallet> = either {
        when (val result = userWalletsListManager.update(userWalletId, update)) {
            is CompletionResult.Failure -> raise(UpdateWalletError.DataError(result.error))
            is CompletionResult.Success -> result.data
        }
    }
}