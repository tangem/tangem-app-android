package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import com.tangem.common.CompletionResult
import com.tangem.domain.core.wallets.error.SelectWalletError
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.core.wallets.UserWalletsListRepository

/**
 * Use case for selecting wallet
 *
 * @property userWalletsListManager user wallets list manager
 * @property reduxStateHolder       redux state holder
 *
[REDACTED_AUTHOR]
 */
class SelectWalletUseCase(
    private val userWalletsListManager: UserWalletsListManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val useNewRepository: Boolean,
    private val reduxStateHolder: ReduxStateHolder,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<SelectWalletError, UserWallet> {
        if (useNewRepository) {
            return userWalletsListRepository.select(userWalletId).map {
                reduxStateHolder.onUserWalletSelected(it)
                it
            }
        }

        return either {
            return when (val result = userWalletsListManager.select(userWalletId)) {
                is CompletionResult.Failure -> raise(SelectWalletError.UnableToSelectUserWallet)
                is CompletionResult.Success -> {
                    reduxStateHolder.onUserWalletSelected(result.data)
                    result.data.right()
                }
            }
        }
    }
}