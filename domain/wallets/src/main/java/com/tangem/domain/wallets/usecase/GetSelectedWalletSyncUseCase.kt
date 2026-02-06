package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.models.GetUserWalletError

/**
 * Use case for getting selected wallet.
 * Important! If all wallets is locked, use case returns a error.
 *
 * @property userWalletsListRepository repository for getting list of user wallets
 *
[REDACTED_AUTHOR]
 */
class GetSelectedWalletSyncUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    @Deprecated("You should provide the selected wallet via routing parameters due to the scalability of the features")
    operator fun invoke(): Either<GetUserWalletError, UserWallet> {
        return either {
            userWalletsListRepository.selectedUserWallet.value ?: raise(GetUserWalletError.UserWalletNotFound)
        }
    }
}