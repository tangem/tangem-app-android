package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.GetUserWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.core.wallets.UserWalletsListRepository

/**
 * Use case for getting selected wallet.
 * Important! If all wallets is locked, use case returns a error.
 *
 * @property userWalletsListManager user wallets list manager
 *
[REDACTED_AUTHOR]
 */
class GetSelectedWalletSyncUseCase(
    private val userWalletsListManager: UserWalletsListManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val useNewRepository: Boolean = false,
) {

    @Deprecated("You should provide the selected wallet via routing parameters due to the scalability of the features")
    operator fun invoke(): Either<GetUserWalletError, UserWallet> {
        if (useNewRepository) {
            return either {
                userWalletsListRepository.selectedUserWallet.value ?: raise(GetUserWalletError.UserWalletNotFound)
            }
        }

        return either {
            ensureNotNull(
                value = userWalletsListManager.selectedUserWalletSync,
                raise = { GetUserWalletError.UserWalletNotFound },
            )
        }
    }
}