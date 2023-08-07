package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.legacy.asLockable
import com.tangem.domain.wallets.models.UnlockWalletError

/**
 * Unlock wallets use case
 *
 * @property walletsStateHolder wallets state holder
 *
[REDACTED_AUTHOR]
 */
class UnlockWalletsUseCase(private val walletsStateHolder: WalletsStateHolder) {

    suspend operator fun invoke(): Either<UnlockWalletError, Unit> {
        val userWalletsListManager = walletsStateHolder.userWalletsListManager?.asLockable()
            ?: return UnlockWalletError.CommonError.left()

        userWalletsListManager.unlock()
            .doOnSuccess { return Unit.right() }
            .doOnFailure { return UnlockWalletError.CommonError.left() }

        return Unit.right()
    }
}