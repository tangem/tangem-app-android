package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.GetUserWalletError
import com.tangem.domain.models.wallet.UserWallet
import kotlinx.coroutines.flow.Flow

/**
 * Use case for getting flow of selected wallet.
 *
 * @property userWalletsListManager user wallets list manager
 *
[REDACTED_AUTHOR]
 */
class GetSelectedWalletUseCase(private val userWalletsListManager: UserWalletsListManager) {

    @Deprecated("You should provide the selected wallet via routing parameters due to the scalability of the features")
    operator fun invoke(): Either<GetUserWalletError, Flow<UserWallet>> {
        return either {
            userWalletsListManager.selectedUserWallet
        }
    }
}