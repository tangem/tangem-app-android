package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.models.GetSelectedWalletError
import com.tangem.domain.wallets.models.UserWallet

/**
 * Use case for getting selected wallet
 *
 * @property walletsStateHolder state holder for getting static initialized 'userWalletsListManager'
 *
[REDACTED_AUTHOR]
 */
class GetSelectedWalletUseCase(private val walletsStateHolder: WalletsStateHolder) {

    operator fun invoke(): Either<GetSelectedWalletError, UserWallet> {
        return either {
            val userWalletsListManager = ensureNotNull(
                value = walletsStateHolder.userWalletsListManager,
                raise = { GetSelectedWalletError.DataError },
            )

            ensureNotNull(
                value = userWalletsListManager.selectedUserWalletSync,
                raise = { GetSelectedWalletError.NoUserWalletSelected },
            )
        }
    }
}