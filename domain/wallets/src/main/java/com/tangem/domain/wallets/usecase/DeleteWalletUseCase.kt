package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.models.DeleteWalletError
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Use case for updating user wallet
 *
 * @property walletsStateHolder state holder for getting static initialized 'userWalletsListManager'
 *
[REDACTED_AUTHOR]
 */
class DeleteWalletUseCase(private val walletsStateHolder: WalletsStateHolder) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<DeleteWalletError, Unit> {
        val userWalletsListManager = walletsStateHolder.userWalletsListManager
            ?: return DeleteWalletError.DataError.left()

        userWalletsListManager.delete(userWalletIds = listOf(userWalletId))
            .doOnSuccess { return Unit.right() }
            .doOnFailure { return DeleteWalletError.DataError.left() }

        return Unit.right()
    }
}