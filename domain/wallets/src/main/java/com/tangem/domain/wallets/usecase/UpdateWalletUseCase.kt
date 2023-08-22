package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.domain.wallets.legacy.WalletsStateHolder
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
    ): Either<UpdateWalletError, Unit> {
        val userWalletsListManager = walletsStateHolder.userWalletsListManager
            ?: return UpdateWalletError.DataError.left()

        userWalletsListManager.update(userWalletId, update)
            .doOnSuccess { return Unit.right() }
            .doOnFailure { return UpdateWalletError.DataError.left() }

        return Unit.right()
    }
}