package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.legacy.ensureUserWalletListManagerNotNull
import com.tangem.domain.wallets.models.GetUserWalletError
import com.tangem.domain.wallets.models.UserWallet
import kotlinx.coroutines.flow.Flow

/**
 * Use case for getting flow of selected wallet.
 *
 * @property walletsStateHolder state holder for getting static initialized 'userWalletsListManager'
 *
[REDACTED_AUTHOR]
 */
class GetSelectedWalletUseCase(private val walletsStateHolder: WalletsStateHolder) {

    operator fun invoke(): Either<GetUserWalletError, Flow<UserWallet>> {
        return either {
            val userWalletsListManager = ensureUserWalletListManagerNotNull(
                walletsStateHolder = walletsStateHolder,
                raise = GetUserWalletError::DataError,
            )

            userWalletsListManager.selectedUserWallet
        }
    }
}