package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.GetUserWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.core.wallets.UserWalletsListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

/**
 * Use case for getting flow of selected wallet.
 *
 * @property userWalletsListManager user wallets list manager
 *
[REDACTED_AUTHOR]
 */
@Deprecated("You should provide the selected wallet via routing parameters due to the scalability of the features")
class GetSelectedWalletUseCase(
    private val userWalletsListManager: UserWalletsListManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val useNewRepository: Boolean = false,
) {

    @Deprecated("You should provide the selected wallet via routing parameters due to the scalability of the features")
    operator fun invoke(): Either<GetUserWalletError, Flow<UserWallet>> {
        return either {
            if (useNewRepository) {
                userWalletsListRepository.selectedUserWallet.filterNotNull()
            } else {
                userWalletsListManager.selectedUserWallet
            }
        }
    }

    @Deprecated("You should provide the selected wallet via routing parameters due to the scalability of the features")
    fun sync(): Either<GetUserWalletError, UserWallet?> {
        return either {
            if (useNewRepository) {
                userWalletsListRepository.selectedUserWallet.value
            } else {
                userWalletsListManager.selectedUserWalletSync
            }
        }
    }
}