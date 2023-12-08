package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import com.tangem.common.CompletionResult
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.legacy.ensureUserWalletListManagerNotNull
import com.tangem.domain.wallets.models.SelectWalletError
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Use case for selecting wallet
 *
 * @property walletsStateHolder state holder for getting static initialized 'userWalletsListManager'
 *
[REDACTED_AUTHOR]
 */
class SelectWalletUseCase(
    private val walletsStateHolder: WalletsStateHolder,
    private val reduxStateHolder: ReduxStateHolder,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<SelectWalletError, UserWallet> {
        return either {
            val userWalletsListManager = ensureUserWalletListManagerNotNull(
                walletsStateHolder = walletsStateHolder,
                raise = { SelectWalletError.DataError },
            )

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