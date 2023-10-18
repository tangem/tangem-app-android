package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.legacy.ensureUserWalletListManagerNotNull
import com.tangem.domain.wallets.models.UpdateWalletError
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Use case for updating user wallet
 *
 * @property walletsStateHolder state holder for getting static initialized 'userWalletsListManager'
 *
[REDACTED_AUTHOR]
 */
class UpdateWalletUseCase(private val walletsStateHolder: WalletsStateHolder) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        update: suspend (UserWallet) -> UserWallet,
    ): Either<UpdateWalletError, UserWallet> {
        return either {
            val userWalletsListManager = ensureUserWalletListManagerNotNull(
                walletsStateHolder = walletsStateHolder,
                raise = { UpdateWalletError.DataError },
            )

            userWalletsListManager.update(userWalletId, update)
                .doOnSuccess { return it.right() }
                .doOnFailure { return UpdateWalletError.DataError.left() }

            return UpdateWalletError.DataError.left()
        }
    }
}