package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.legacy.ensureUserWalletListManagerNotNull
import com.tangem.domain.wallets.models.GetUserWalletError
import com.tangem.domain.wallets.models.UserWallet

/**
 * Use case for getting selected wallet.
 * Important! If all wallets is locked, use case returns a error.
 *
 * @property walletsStateHolder state holder for getting static initialized 'userWalletsListManager'
 *
[REDACTED_AUTHOR]
 */
class GetSelectedWalletSyncUseCase(private val walletsStateHolder: WalletsStateHolder) {

    operator fun invoke(): Either<GetUserWalletError, UserWallet> {
        return either {
            val userWalletsListManager = ensureUserWalletListManagerNotNull(
                walletsStateHolder = walletsStateHolder,
                raise = GetUserWalletError::DataError,
            )

            ensureNotNull(
                value = userWalletsListManager.selectedUserWalletSync,
                raise = { GetUserWalletError.UserWalletNotFound },
            )
        }
    }
}