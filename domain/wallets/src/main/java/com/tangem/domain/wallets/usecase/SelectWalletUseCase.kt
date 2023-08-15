package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.right
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.models.SelectWalletError
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Use case for selecting wallet
 *
 * @property walletsStateHolder state holder for getting static initialized 'userWalletsListManager'
 *
* [REDACTED_AUTHOR]
 */
class SelectWalletUseCase(private val walletsStateHolder: WalletsStateHolder) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<SelectWalletError, Unit> {
        return either {
            val userWalletsListManager = ensureNotNull(
                value = walletsStateHolder.userWalletsListManager,
                raise = { SelectWalletError.DataError },
            )

            userWalletsListManager.select(userWalletId)
                .doOnSuccess { return Unit.right() }
                .doOnFailure { return SelectWalletError.UnableToSelectUserWallet.left() }

            return Unit.right()
        }
    }
}
