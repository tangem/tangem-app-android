package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.legacy.ensureUserWalletListManagerNotNull
import com.tangem.domain.wallets.models.DeleteWalletError
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Use case for updating user wallet
 *
 * @property walletsStateHolder state holder for getting static initialized 'userWalletsListManager'
 *
 * @author Andrew Khokhlov on 07/07/2023
 */
class DeleteWalletUseCase(private val walletsStateHolder: WalletsStateHolder) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<DeleteWalletError, Unit> {
        return either {
            val userWalletsListManager = ensureUserWalletListManagerNotNull(
                walletsStateHolder = walletsStateHolder,
                raise = { DeleteWalletError.DataError },
            )

            userWalletsListManager.delete(userWalletIds = listOf(userWalletId))
                .doOnSuccess { return Unit.right() }
                .doOnFailure { return DeleteWalletError.UnableToDelete.left() }

            return Unit.right()
        }
    }
}
