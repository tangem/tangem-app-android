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
 * @author Andrew Khokhlov on 08/08/2023
 */
class GetSelectedWalletSyncUseCase(private val userWalletsListManager: UserWalletsListManager) {

    @Deprecated("You should provide the selected wallet via routing parameters due to the scalability of the features")
    operator fun invoke(): Either<GetUserWalletError, UserWallet> {
        return either {
            ensureNotNull(
                value = userWalletsListManager.selectedUserWalletSync,
                raise = { GetUserWalletError.UserWalletNotFound },
            )
        }
    }
}
