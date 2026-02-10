package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.models.GetUserWalletError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

/**
 * Use case for getting flow of selected wallet.
 *
 * @property userWalletsListRepository repository for getting list of user wallets
 *
[REDACTED_AUTHOR]
 */
@Deprecated("You should provide the selected wallet via routing parameters due to the scalability of the features")
class GetSelectedWalletUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    @Deprecated("You should provide the selected wallet via routing parameters due to the scalability of the features")
    operator fun invoke(): Either<GetUserWalletError, Flow<UserWallet>> {
        return either {
            userWalletsListRepository.selectedUserWallet.filterNotNull()
        }
    }

    @Deprecated("You should provide the selected wallet via routing parameters due to the scalability of the features")
    fun sync(): Either<GetUserWalletError, UserWallet?> {
        return either {
            userWalletsListRepository.selectedUserWallet.value
        }
    }
}