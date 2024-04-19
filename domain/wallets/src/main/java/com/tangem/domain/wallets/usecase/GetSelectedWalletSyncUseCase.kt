package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.GetUserWalletError
import com.tangem.domain.wallets.models.UserWallet

/**
 * Use case for getting selected wallet.
 * Important! If all wallets is locked, use case returns a error.
 *
 * @property userWalletsListManager user wallets list manager
 *
* [REDACTED_AUTHOR]
 */
class GetSelectedWalletSyncUseCase(private val userWalletsListManager: UserWalletsListManager) {

    operator fun invoke(): Either<GetUserWalletError, UserWallet> {
        return either {
            ensureNotNull(
                value = userWalletsListManager.selectedUserWalletSync,
                raise = { GetUserWalletError.UserWalletNotFound },
            )
        }
    }
}
